# AI-003 — Central Simulator (M3)

- Task / spec / ADR: SPEC-005/006, ADR-004/009, OpenAPI Central
- Tool and model: Cursor agent (Composer)
- Skills loaded: java-engineering, spring-boot-engineering, security-engineering, idempotency-engineering, test-engineering, ai-governance
- Constraints: HMAC canonical documented before coding; no DB txn across Central HTTP (N/A inside simulator); synthetic data only; deterministic faults via header/beneficiary suffix

## Changes

- Flyway `V2__central_protocol.sql` — authorizations, agenda, protocols, reservations, idempotency, nonces + seed parcels
- Integrity: `HmacCanonical`, `HmacService`, `NonceReplayGuard`, `HmacFilter` + cached body
- Protocol: authorization, agenda, portability create/status with confirm delay and fault injection (`X-Sim-Fault` / beneficiary suffix)
- Docs: `docs/architecture/hmac-canonical.md`
- Tests: `HmacCanonicalTest`, `CentralHmacIntegrationTest` (HMAC reject, replay, idempotency, rejection fault)

## Validation

```text
$ java -version
openjdk version "25.0.5.1" 2026-11-17 LTS (Corretto)

$ cd apps/central-simulator && ./mvnw -B verify
Flyway: Successfully applied 2 migrations ... now at version v2
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Notes: Spring Boot 4 uses Jackson 3 (`tools.jackson.*`) via `spring-boot-starter-jackson`; integration tests use MockMvc (TestRestTemplate removed in Boot 4). Authorization upsert uses `ON CONFLICT` to avoid aborted Postgres transactions after duplicate key.

- Validation: PASS
- Human review / decision: pending
- Commit reference: pending (user-executed)
- Remaining risks: portability-api does not call Central yet (M4); response-signing verification by clients deferred; fault modes beyond REJECT/503 not exhaustively covered
