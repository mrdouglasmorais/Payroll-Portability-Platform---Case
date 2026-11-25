# AI-002 — Domain & Persistence (M2)

- Task / spec / ADR: SPEC-001/002/003/005/007, ADR-003/005/006/007/008/011
- Tool and model: Cursor agent (Composer)
- Skills loaded: domain-modeling, database-engineering, java-engineering, spring-boot-engineering, financial-domain, idempotency-engineering, audit-engineering, test-engineering, ai-governance
- Constraints: BigDecimal/NUMERIC only; atomic operation+reservation+event+job; no HTTP to Central in this milestone; terminal states irreversible

## Changes

- Flyway `V2__domain_persistence.sql` — beneficiaries, consents, installments, operations, reservations, events, idempotency keys, processing jobs
- Domain packages: `consent`, `schedule`, `portability`, `shared` (Money)
- JDBC adapters + `CreatePortabilityService` (atomic create, idempotency, consent/eligibility checks)
- Tests: Money, operation state machine, CreatePortabilityServiceIT (idempotency + concurrent reservation)

## Validation

```text
$ java -version
openjdk version "25.0.5.1" 2026-11-17 LTS (Corretto)

$ cd apps/portability-api && ./mvnw -B verify
Flyway: Successfully applied 2 migrations ... now at version v2
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Note: Spring Boot 4 requires `spring-boot-starter-flyway` (flyway-core alone does not auto-configure).

- Validation: PASS
- Human review / decision: pending
- Commit reference: pending (user-executed)
- Remaining risks: no HTTP API for domain yet (M4); Central schema still M3; settlement package deferred inside portability

