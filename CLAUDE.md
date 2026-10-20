# ticketing-platform

Event ticketing platform with seat reservation. This is a **portfolio project** meant to
demonstrate production-grade backend skills: microservices, hexagonal architecture, event-driven
communication with Kafka, Kubernetes deployment, testing and observability.

The owner is learning Spring Boot / Java tooling while building this. When making changes, **explain the why** of
non-obvious decisions, keep PRs small and focused, and prefer clarity
over cleverness. Scope discipline matters: 4–5 polished services beat 10 half-done ones.

Repo: https://github.com/hectorplinio/ticketing-platform

---

## Tech stack

| Area               | Choice                                                                                                                                 |
|--------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| Language           | Java 21 (Temurin, managed with `mise`)                                                                                                 |
| Framework          | Spring Boot 4.1.1, Spring Cloud 2025.1.3 (gateway only)                                                                                |
| Build              | Gradle 9.8.1, Kotlin DSL, multi-module monorepo, wrapper committed                                                                     |
| Formatting         | Spotless 8.10.4 + palantir-java-format, configured once in root `build.gradle.kts` via `subprojects { plugins.withId("java") { ... } }` |
| Messaging          | Apache Kafka (KRaft, no Zookeeper)                                                                                                     |
| Database           | PostgreSQL 17, one database per service, Flyway migrations                                                                             |
| Cache / rate limit | Redis 7                                                                                                                                |
| Identity           | Keycloak (OIDC) — planned, not yet integrated                                                                                          |
| Email (local)      | Mailpit (SMTP 1025, UI 8025)                                                                                                           |
| Tests              | JUnit 5, Testcontainers (real Kafka/Postgres), ArchUnit (planned)                                                                      |
| Observability      | OpenTelemetry + Grafana + Tempo — planned                                                                                              |
| Deploy             | Kubernetes (kind/OrbStack locally), Helm charts, Strimzi (Kafka), CloudNativePG — planned                                              |
| CI                 | GitHub Actions                                                                                                                         |

Gradle settings (`gradle.properties`): configuration cache, parallel and build cache enabled.

---

## Repository layout

```
ticketing-platform/
├── CLAUDE.md
├── settings.gradle.kts          # includes services:gateway, catalog, reservation, payment, notification
├── build.gradle.kts             # group/version + Spotless for all Java subprojects
├── gradle.properties
├── gradle/wrapper/              # the ONLY wrapper in the repo
├── infra/
│   ├── docker-compose.yml       # kafka, postgres, redis, mailpit
│   └── postgres/init.sql        # creates catalog, reservation, payment DBs (runs only on empty volume)
├── services/
│   ├── gateway/
│   ├── catalog/
│   ├── reservation/
│   ├── payment/
│   └── notification/
├── .github/
│   ├── workflows/ci.yml         # spotlessCheck + ./gradlew build on push to main and on PRs
│   └── dependabot.yml           # weekly, gradle deps grouped + github-actions
└── (planned) build-logic/, libs/event-contracts/, deploy/helm/, README.md
```

Each service has its own `build.gradle.kts` but **no** `settings.gradle.kts`, `gradlew` or
`gradle/` folder — those live only at the root. Do not regenerate them inside services.

---

## Services

| Service      | App port | Mgmt port | DB          | Responsibility                                                                                                                                  |
|--------------|----------|-----------|-------------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| gateway      | 8080     | 9080      | —           | Single external entry point. Routing, JWT validation (Keycloak), rate limiting (Redis). No business logic.                                      |
| catalog      | 8081     | 9081      | catalog     | Events, venues, seat maps, prices.                                                                                                              |
| reservation  | 8082     | 9082      | reservation | **Core of the project.** Seat holds with TTL (~10 min), concurrency (two users, same seat → only one wins), automatic release of expired holds. |
| payment      | 8083     | 9083      | payment     | Simulated payment gateway that fails randomly to exercise saga compensation.                                                                    |
| notification | 8084     | 9084      | —           | Generates ticket with QR and sends it by email (Mailpit locally).                                                                               |

Dependencies per service:

- catalog, reservation, payment: webmvc, data-jpa, flyway (+ flyway-database-postgresql), postgresql, validation, kafka,
  actuator, Testcontainers (kafka + postgresql).
- notification: webmvc, kafka, mail, actuator, Testcontainers (kafka). Has webmvc only so Actuator can serve probes.
- gateway: spring-cloud-starter-gateway-server-webflux (reactive), oauth2-resource-server, actuator.

Each business service's tests use a `TestcontainersConfiguration` (`@TestConfiguration` with
`@ServiceConnection` beans) imported by `*ApplicationTests`, plus a `Test*Application` main for
running locally with containers.

---

## Communication

**Synchronous (internal REST)** — only when the caller needs the answer right now.
Example: reservation asks catalog whether a seat exists and its price. Use `RestClient`,
target via Kubernetes DNS (`http://catalog:8081`) configured by env var with a localhost default.

**Asynchronous (Kafka)** — everything else. Choreographed saga (no central orchestrator):

```
reservation ──SeatsHeld──────────▶ payment
payment ─────PaymentCompleted────▶ reservation ──ReservationConfirmed──▶ notification
payment ─────PaymentFailed───────▶ reservation (releases seats)
reservation ─ReservationExpired──▶ (seats released by TTL)
```

Reliability rules:

- Publish events with the **Outbox pattern** (state change + event in the same DB transaction).
- Consumers must be **idempotent** (dedupe by event id).
- Failed messages go to a **DLQ**.
- Event schemas will live in `libs/event-contracts` (Avro or JSON Schema — not decided yet).

---

## Architecture rules (hexagonal)

Applies to catalog, reservation, payment and notification. Base package:
`com.hector.ticketing.<service>`. The `*Application` class stays in the base package.

```
com.hector.ticketing.<service>/
├── domain/
│   ├── <aggregate>/       # entities, value objects, domain events, repository INTERFACES (output ports)
│   └── shared/            # DomainEvent, EventPublisher interface, base exceptions
├── application/
│   └── <aggregate>/       # use cases as concrete classes, e.g. HoldSeats, ConfirmReservation
└── infrastructure/
    ├── input/
    │   ├── rest/          # controllers
    │   └── kafka/         # consumers
    ├── output/
    │   ├── persistence/   # JPA entities + adapters implementing domain repository interfaces
    │   ├── kafka/         # outbox + publisher, KafkaHealthIndicator
    │   └── <client>/      # REST clients to other services
    └── config/            # Spring beans wiring
```

Rules:

- `domain` is **pure Java**: no Spring, no JPA annotations, no imports from `application` or `infrastructure`.
- `application` may depend on `domain` only. No framework types in use case signatures.
- Use cases are **concrete classes**, no input-port interface with a single implementation.
- Domain services (logic spanning several entities) go in `domain`, never use cases.
- JPA entities are separate from domain entities; adapters map between them.
- Aggregate subpackages are created **when the aggregate is modeled**, not up front.
- Prefer Java `record` for value objects, commands and DTOs. **No Lombok.**
- Planned: ArchUnit tests enforcing these rules in every service.

Gateway is **not** hexagonal (it has no domain). It only has `config/` and `filter/` packages.

---

## Health checks (decided)

- Every service uses **Spring Boot Actuator**; no custom health endpoint.
- Actuator runs on a **separate management port** (`management.server.port`, 908x) so business
  routes in the gateway can never accidentally expose `/actuator/*`.
- Exposed endpoints: `health` only. Probes enabled (Boot default): `/actuator/health/liveness`,
  `/actuator/health/readiness`.
- `show-details: never` — externally only `{"status":"UP"}` / `{"status":"DOWN"}`.
- Liveness must never depend on DB/Kafka (avoid restart loops); readiness does:
  `management.endpoint.health.group.readiness.include` is `readinessState,db,kafka` in catalog,
  reservation and payment, and `readinessState,kafka` in notification (no DB). Contributor
  names are case-sensitive and validated at startup — an unknown name fails the context.
- Spring Boot has **no built-in Kafka health indicator**. Each business service has a
  `KafkaHealthIndicator` in `infrastructure/output/kafka/` (contributor name `kafka`): `describeCluster`
  via `AdminClient` with a 1 s timeout, below the gateway's 2 s response timeout. It implements no
  domain port (Actuator calls it, not the domain); it lives in `output/kafka/` to sit next to the
  Kafka adapter it checks. Its name must stay `KafkaHealthIndicator`: Boot derives the contributor
  name from the bean name. Duplicated per service until a shared module exists.
- Externally, health is reachable **only** through gateway routes, one per service:
  `/health/gateway`, `/health/catalog`, `/health/reservation`, `/health/payment`, `/health/notification`.
  Each route: `SetPath=/actuator/health/readiness` (readiness checks DB/Kafka, and its response has
  no `groups` field), `uri: http://localhost:908x` (hardcoded for now; will become an env var for
  Kubernetes), route metadata `connect-timeout: 1000`, `response-timeout: 2000`.
- Known limitation: if Postgres stops responding or accepting connections, the `db` check waits on
  Hikari's `connection-timeout` (30 s by default) and the gateway returns `504` instead of
  `503 {"status":"DOWN"}`. Kafka outages always return `503 DOWN` in ~1 s.
- Gateway `SecurityConfig` (`config/`, `SecurityWebFilterChain`): CSRF, HTTP Basic and form login
  disabled; `/health/**` permitAll; Actuator health (`EndpointRequest.to(HealthEndpoint.class)`)
  permitAll because the management port shares this filter chain and `/health/gateway` routes
  back to it; everything else authenticated. No JWT validation yet (no Keycloak), so
  non-health routes return `401`. Will grow with Keycloak JWT validation.
- Kubernetes probes hit pods directly (management port), never through the gateway.
- The owner keeps a Postman collection with these 5 health requests and a test asserting
  `200` + `status == "UP"`.

Spring Cloud Gateway property prefix in this version: `spring.cloud.gateway.server.webflux.routes`.

---

## Local development

```bash
docker compose -f infra/docker-compose.yml up -d    # Kafka 9092, Postgres 5432, Redis 6379, Mailpit 8025/1025
./gradlew spotlessApply                              # format
./gradlew build                                      # compile + tests (needs Docker) + spotlessCheck
```

Postgres local credentials: user `ticketing`, password `ticketing` (local only).
`init.sql` is executed automatically by Postgres on first start; never run it manually.
To re-run it: `docker compose -f infra/docker-compose.yml down -v`.

Environment: macOS (Apple Silicon), IntelliJ IDEA with palantir-java-format plugin and
reformat-on-save, Docker Desktop, Homebrew, mise.

---

## Git and GitHub workflow

- `main` is protected by ruleset `protect-main`: PR required (0 approvals), status check `build`
  must pass, branch up to date, linear history, no force push, no deletion, no bypass.
- Squash merge only; head branches auto-deleted.
- Branch names: `feat/...`, `fix/...`, `chore/...`, `docs/...`, `build/...`, `test/...`.
- **Conventional Commits in English**: `feat:`, `fix:`, `chore:`, `build:`, `docs:`, `test:`, `refactor:`.
- Always run `./gradlew spotlessApply` and `./gradlew build` before committing.
- Never commit secrets. `.env`, `.idea/`, `*.iml`, `build/`, `.gradle` are gitignored.
- Dependabot alerts, secret scanning and push protection are enabled.

---

## Roadmap

| Phase | Deliverable                                                                                                                      | Status         |
|-------|----------------------------------------------------------------------------------------------------------------------------------|----------------|
| 0     | Gradle monorepo skeleton, docker-compose, Spotless, CI, Dependabot, branch protection                                            | ✅ Done        |
| 0.5   | Health probes PR (mgmt ports, gateway `/health/*` routes, SecurityConfig)                                                        | 🔄 In progress |
| 1     | catalog + reservation domain: seat holds, TTL expiry, concurrency (unique constraint + optimistic locking), Testcontainers tests | Next           |
| 2     | Gateway business routes + Keycloak JWT + Redis rate limiting                                                                     | Planned        |
| 3     | payment + full saga with Outbox, idempotent consumers, DLQ                                                                       | Planned        |
| 4     | notification: QR ticket + email                                                                                                  | Planned        |
| 5     | OpenTelemetry traces across Kafka, Grafana dashboards                                                                            | Planned        |
| 6     | Dockerfiles/images, Helm charts, Strimzi, CloudNativePG, deploy to kind/OrbStack from CI                                         | Planned        |

Phase 1 is the showcase: the hard concurrency problem lives there. Even stopping at phase 3
yields a solid portfolio project.

## Known follow-ups (small PRs)

- `chore/pin-versions`: Testcontainers images use `:latest` (`postgres:latest`,
  `apache/kafka-native:latest`). Pin them and align with docker-compose (`postgres:17`, pin Kafka too).
  Add `permissions: contents: read` to `ci.yml`. Fix `### Proyect ###` typo and missing final
  newline in `.gitignore`.
- `docs/readme`: README with architecture diagram, stack, how to run locally, roadmap.
- `build/convention-plugins`: move repeated config (Spring Boot plugin versions, toolchain,
  repositories, `group`/`version`, test setup) from each service's `build.gradle.kts` into
  `build-logic/` convention plugins. `group`/`version` are currently duplicated in root and services.
