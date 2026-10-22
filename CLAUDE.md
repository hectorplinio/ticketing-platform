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
| Tests              | JUnit 5, Testcontainers (real Kafka/Postgres), ArchUnit 1.5.1                                                                          |
| Observability      | Micrometer Tracing + OpenTelemetry bridge (traceId in logs, no exporter yet); Grafana + Tempo — planned                                |
| Deploy             | Kubernetes (kind/OrbStack locally), Helm charts, Strimzi (Kafka), CloudNativePG — planned                                              |
| CI                 | GitHub Actions                                                                                                                         |

Gradle settings (`gradle.properties`): configuration cache, parallel and build cache enabled.

---

## Repository layout

```
ticketing-platform/
├── CLAUDE.md
├── README.md                    # public overview: architecture, stack, how to run, roadmap
├── settings.gradle.kts          # includes libs:platform-commons and services:gateway, catalog, reservation, payment, notification
├── build.gradle.kts             # group/version + Spotless for all Java subprojects
├── gradle.properties
├── gradle/wrapper/              # the ONLY wrapper in the repo
├── infra/
│   ├── docker-compose.yml       # kafka, postgres, redis, mailpit
│   └── postgres/init.sql        # creates catalog, reservation, payment DBs (runs only on empty volume)
├── libs/
│   └── platform-commons/        # shared technical code + Spring Boot auto-configuration (see below)
├── services/
│   ├── gateway/
│   ├── catalog/
│   ├── reservation/
│   ├── payment/
│   └── notification/
├── .github/
│   ├── workflows/ci.yml         # spotlessCheck + ./gradlew build on push to master and on PRs
│   └── dependabot.yml           # weekly, gradle deps grouped + github-actions
└── (planned) build-logic/, libs/event-contracts/, deploy/helm/
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
- catalog, reservation, payment and notification also depend on `libs:platform-commons`. The gateway does not
  (reactive, no domain).
- gateway: spring-cloud-starter-gateway-server-webflux (reactive), oauth2-resource-server, actuator.

Each business service's tests use a `TestcontainersConfiguration` (`@TestConfiguration` with
`@ServiceConnection` beans) imported by `*ApplicationTests`, plus a `Test*Application` main for
running locally with containers. Images are pinned to the docker-compose versions
(`postgres:17`, Kafka `4.3.1`; tests use the `apache/kafka-native` image) with a 3-minute startup
timeout, because in CI all services start their containers in parallel on a small runner. Never use
`:latest`.

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
│   └── shared/            # DomainEvent, EventPublisher interface (base exceptions live in platform-commons)
├── application/
│   └── <aggregate>/       # use cases as concrete classes, e.g. HoldSeats, ConfirmReservation
└── infrastructure/
    ├── input/
    │   ├── rest/          # controllers
    │   └── kafka/         # consumers
    ├── output/
    │   ├── persistence/   # JPA entities + adapters implementing domain repository interfaces
    │   ├── kafka/         # outbox + publisher
    │   └── <client>/      # REST clients to other services
    └── config/            # Spring beans wiring
```

Rules:

- `domain` is **pure Java**: no Spring, no JPA annotations, no imports from `application` or `infrastructure`.
  The only allowed external import is `com.hector.ticketing.commons.domain` (pure Java base exceptions).
- `application` may depend on `domain` only. No framework types in use case signatures.
- Use cases are **concrete classes**, no input-port interface with a single implementation.
- Domain services (logic spanning several entities) go in `domain`, never use cases.
- JPA entities are separate from domain entities; adapters map between them.
- Aggregate subpackages are created **when the aggregate is modeled**, not up front.
- Prefer Java `record` for value objects, commands and DTOs. **No Lombok.**
- **Enforced by ArchUnit**: rules live once in `HexagonalArchitectureRules` (platform-commons
  `testFixtures`, package `commons.architecture`); each business service has an `ArchitectureTest`
  (`@AnalyzeClasses` on its base package, production classes only) that includes them. A violation
  fails `./gradlew build` with the broken rule and the reason. Rules: domain depends only on `java..`
  and domain packages; application only on domain/application; `infrastructure.input` never uses
  `infrastructure.output`; `@Entity` only in `output.persistence`; `@RestController` only in
  `input.rest`; no field injection. Every rule uses `allowEmptyShould(true)` so services without
  code yet still pass. New rules go in `HexagonalArchitectureRules`, never in a single service.

Gateway is **not** hexagonal (it has no domain). It only has `config/` and `filter/` packages.

---

## Shared module: `libs/platform-commons`

Technical building blocks that would otherwise be copied into every business service. Base package
`com.hector.ticketing.commons`:

| Package | Contents | Registered by |
|---|---|---|
| `domain` | `DomainValidationException` (→ 400), `NotFoundException` (abstract, → 404). Pure Java. | — (plain classes) |
| `persistence` | `BaseJpaEntity` | — (services extend it) |
| `web` | `GlobalExceptionHandler`, `DataIntegrityExceptionHandler` (package-private) | `ErrorHandlingAutoConfiguration`: servlet apps only, backs off if the service defines its own `ResponseEntityExceptionHandler`; the data handler only when spring-tx is present |
| `kafka` | `KafkaHealthIndicator` (package-private) | `KafkaHealthAutoConfiguration`: only when a `KafkaAdmin` bean exists; bean name `kafkaHealthIndicator` → contributor `kafka` |

Rules:

- **Only technical code, never business/domain code of a service.** Sharing a domain model would
  couple the services' releases.
- Beans are contributed through **Spring Boot auto-configuration**
  (`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`), so a service
  gets them just by depending on the module: no `@Import`, no component scan of other packages.
  Each auto-configuration is guarded by `@Conditional*` so it only activates when the service has
  the matching technology (e.g. notification has no JPA).
- Spring starters are `compileOnly`: the module never adds dependencies to a service.
- Auto-configurations are tested with `ApplicationContextRunner` (conditions only, no containers).
- `testFixtures` holds test code reused by the services' tests (ArchUnit rules). It is not part of
  the module's jar; services consume it with `testImplementation(testFixtures(project(":libs:platform-commons")))`.

---

## Aggregate template (reference: `Venue` in catalog)

Every new aggregate follows the same files and decisions as `Venue`. The owner models the next
aggregates (Event, Seat...) themselves, using Venue as the reference.

| Layer | Files | Decisions |
|---|---|---|
| `domain/<agg>/` | `Venue`, `VenueRepository`, `VenueNotFoundException` | Entity is a `class` (not record): `final UUID id`, mutable fields, constructor that validates (rebuild from DB), `static create(...)` that generates the UUID, behaviour methods (`update`) that validate **everything before assigning**. Max lengths as `public static final` constants. Port uses only domain + JDK types; `save` covers insert and update; `findById` returns `Optional`. |
| `platform-commons` `domain` | `DomainValidationException` (→ 400), `NotFoundException` (abstract, → 404) | Domain never throws `IllegalArgumentException` for business rules. |
| `platform-commons` `persistence` | `BaseJpaEntity` | `@MappedSuperclass`: `id` (assigned, no `@GeneratedValue`), `created_at` (`@CreationTimestamp`, `updatable = false`), `updated_at` (`@UpdateTimestamp`). `Instant` ↔ `timestamptz`. Implements `Persistable` so inserts skip the extra SELECT. `equals`/`hashCode` by id, proxy-safe. |
| `infrastructure/output/persistence/<agg>/` | `VenueJpaEntity`, `VenueJpaRepository` (package-private), `VenuePersistenceAdapter` | Entity extends `BaseJpaEntity`, declares only its own columns, no validation, `@Column(length = Venue.X_MAX_LENGTH)`. Adapter `save` loads the existing entity and calls `update(...)` (keeps `created_at`), else persists a new one. |
| `resources/db/migration/` | `V<n>__<description>.sql` | Hand-written. Always includes `id uuid`, `created_at`/`updated_at timestamptz NOT NULL DEFAULT now()`. Named constraints (`pk_<table>`, `ck_<table>_<rule>`). Lengths and CHECKs mirror the domain rules. Never edit a merged migration: add a new version. `ddl-auto: validate` fails startup on any mismatch. |
| `application/<agg>/` | `CreateVenue`, `GetVenue`, `ListVenues`, `UpdateVenue`, `DeleteVenue`, `*Command` records | Plain Java, no Spring annotations, method `execute(...)`. Not-found → aggregate's `NotFoundException`. |
| `infrastructure/config/` | `VenueUseCaseConfig` | `@Bean` per use case, so `application` stays framework-free. |
| `infrastructure/input/rest/<agg>/` | `VenueController`, `VenueRequest`, `VenueResponse` | Package-private. Request record with Bean Validation (wrapper types like `Integer` so missing fields are 400) reusing the domain length constants; response record with `static from(domain)`. `POST` → `201` + `Location`, `DELETE` → `204`. |
| `platform-commons` `web` | `GlobalExceptionHandler` (auto-configured) | RFC 9457 `ProblemDetail`; validation errors add an `errors` map (field → message). |
| tests | `domain/<agg>/VenueTest`, `infrastructure/input/rest/<agg>/VenueControllerTest` | Domain: pure JUnit + AssertJ, no Spring. API: `@SpringBootTest` + `@AutoConfigureMockMvc` + `MockMvcTester` + `TestcontainersConfiguration`, end to end against real Postgres. |

Known trade-offs: an update runs two SELECTs (use case `findById` + adapter `save`) because use
cases are not transactional; no optimistic locking yet (`@Version` will come with reservation,
where concurrent writes matter); `GET` list has no pagination yet.

---

## Error handling and logging (decided)

All business services get this from `platform-commons`; nothing to write per service.

| Exception | Status | Title | Logged as |
|---|---|---|---|
| `NotFoundException` (domain) | 404 | Resource not found | WARN, one line |
| `DomainValidationException` (domain) | 400 | Invalid request | WARN, one line |
| Bean Validation (`@Valid`) | 400 | Invalid request + `errors` map | WARN, one line |
| Spring MVC errors (malformed JSON, bad UUID, 405...) | 4xx | from the base class | WARN, one line |
| `DataIntegrityViolationException` (FK, unique, CHECK) | 409 | Conflict | WARN, one line |
| anything else | 500 | Internal server error | **ERROR with stack trace** |

- 4xx are client mistakes, not system failures: never ERROR, so alerts only fire for real problems.
- Responses never expose internals: the 409 and 500 bodies are generic; the database message and
  the stack trace go only to the log.
- `DataIntegrityExceptionHandler` is a separate advice because `DataIntegrityViolationException`
  lives in spring-tx, absent in notification. It implements `Ordered` (highest precedence): Spring
  asks advices in order and the first with a matching handler wins, so otherwise the catch-all
  `Exception` handler would turn every 409 into a 500.
- Logging is SLF4J + Logback (Boot default). Classes log with
  `private static final Logger log = LoggerFactory.getLogger(X.class);`. No custom logger.
- **Tracing:** every service (gateway included) has `spring-boot-micrometer-tracing-opentelemetry` +
  `micrometer-tracing-bridge-otel`. Each request gets a `traceId`/`spanId` that Boot adds to every
  log line, and the W3C `traceparent` header is propagated to downstream calls (and reused when it
  arrives). No exporter until phase 5 (an OTLP exporter without a collector would log connection errors).
- **Structured logs:** text by default (readable locally). Set `LOGGING_STRUCTURED_FORMAT_CONSOLE=ecs`
  to log one JSON object per line (Elastic Common Schema) with `traceId`/`spanId` as fields; this is
  what Kubernetes will use. Configured by env var, not by profile, so no yaml changes per service.

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
- Spring Boot has **no built-in Kafka health indicator**. `KafkaHealthIndicator` lives in
  `libs/platform-commons` (`commons.kafka`) and is auto-configured in every service that has a
  `KafkaAdmin` (contributor name `kafka`): `describeCluster` via `AdminClient` with a 1 s timeout,
  below the gateway's 2 s response timeout. It implements no domain port (Actuator calls it, not the
  domain). The bean method must stay `kafkaHealthIndicator`: Boot derives the contributor name from
  the bean name.
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
| 0.5   | Health probes PR (mgmt ports, gateway `/health/*` routes, SecurityConfig)                                                        | ✅ Done        |
| 1     | catalog + reservation domain: seat holds, TTL expiry, concurrency (unique constraint + optimistic locking), Testcontainers tests | Next           |
| 2     | Gateway business routes + Keycloak JWT + Redis rate limiting                                                                     | Planned        |
| 3     | payment + full saga with Outbox, idempotent consumers, DLQ                                                                       | Planned        |
| 4     | notification: QR ticket + email                                                                                                  | Planned        |
| 5     | OpenTelemetry traces across Kafka, Grafana dashboards                                                                            | Planned        |
| 6     | Dockerfiles/images, Helm charts, Strimzi, CloudNativePG, deploy to kind/OrbStack from CI                                         | Planned        |

Phase 1 is the showcase: the hard concurrency problem lives there. Even stopping at phase 3
yields a solid portfolio project.

## Known follow-ups (small PRs)

- `build/convention-plugins`: move repeated config (Spring Boot plugin versions, toolchain,
  repositories, `group`/`version`, test setup) from each service's `build.gradle.kts` into
  `build-logic/` convention plugins. `group`/`version` are currently duplicated in root and services.
