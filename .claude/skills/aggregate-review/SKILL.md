---
name: aggregate-review
description: Review a hand-written aggregate (e.g. Event, Seat) against the Venue reference template in CLAUDE.md, layer by layer. Reports findings only; never edits code.
disable-model-invocation: true
argument-hint: <AggregateName> [service, default catalog]
---

# Aggregate review

The owner writes aggregates by hand to learn, using `Venue` (catalog) as the reference.
Your job is to review, not to write: **do not edit any file**. Point out what deviates
from the template and why it matters, so the owner fixes it themselves.

Target: aggregate `$ARGUMENTS` (service defaults to `catalog` if not given).

## Steps

1. Read the "Aggregate template" and "Architecture rules" sections of `CLAUDE.md`. They are
   the source of truth; this skill only lists what to check.
2. Find the aggregate's files (`find services/<service> -ipath '*<aggregate>*'`) and the
   matching Venue files. Read both in full and compare them layer by layer.
3. Check each item below. For each one, compare with how Venue does it, not with general taste.

## Checklist

**Domain** (`domain/<agg>/`)
- `class` (not record), `final UUID id`, rebuild constructor that validates, `static create(...)` that generates the UUID.
- Behaviour methods validate **everything before assigning** (no half-updated entity on failure).
- Throws `DomainValidationException` / an aggregate `NotFoundException`, never `IllegalArgumentException`.
- Max lengths as `public static final` constants; repository port uses only domain + JDK types, `findById` returns `Optional`.
- References to other aggregates by id, not by object.

**Persistence** (`infrastructure/output/persistence/<agg>/`)
- JPA entity extends `BaseJpaEntity`, only its own columns, `@Column(length = <Agg>.X_MAX_LENGTH)`, no validation.
- `JpaRepository` package-private; adapter `save` updates the existing entity (keeps `created_at`) or persists a new one.

**Migration** (`db/migration/`)
- New `V<n>__<description>.sql`, never an edited merged one.
- `id uuid`, `created_at`/`updated_at timestamptz NOT NULL DEFAULT now()`, named constraints (`pk_`, `fk_`, `ck_<table>_<rule>`).
- Lengths and CHECKs mirror the domain rules exactly (`ddl-auto: validate` only catches types, not CHECKs).
- Foreign keys indexed when queried by them.

**Application** (`application/<agg>/`)
- Concrete use cases with `execute(...)`, no Spring annotations, `*Command` records.
- Not-found → the aggregate's `NotFoundException`.

**Wiring** (`infrastructure/config/<Agg>UseCaseConfig`): one `@Bean` per use case.

**REST** (`infrastructure/input/rest/<agg>/`)
- Package-private controller, request/response records; Bean Validation with wrapper types, reusing domain length constants.
- `POST` → `201` + `Location`, `DELETE` → `204`, response `static from(domain)`.

**Tests**
- Domain test: pure JUnit + AssertJ, no Spring; covers every validation rule and the validate-before-assign behaviour of `update`.
- Controller test: `@SpringBootTest` + `MockMvcTester` + `TestcontainersConfiguration`; covers 201/200/204, 400 with `errors` map, 404, and 409 where a constraint can fail.

## Output

Reply in Spanish (code identifiers stay as they are). Group findings by layer, most
important first, each with `file:line`, what differs from Venue, and **why** it matters.
Mark each as **bug** (wrong behaviour), **template** (deviates from the convention) or
**nit**. If a deviation from Venue is a deliberate and better choice, say so instead of
flagging it. End with a one-line verdict. Do not write the fixes.
