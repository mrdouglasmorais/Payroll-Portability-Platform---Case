# AI-007 — Failure recovery (M7)

- Task / spec / ADR: SPEC-008, acceptance gate “timeout recovers protocol after retry/restart”, Miro frames 09/17
- Tool and model: Cursor agent (Composer)
- Skills loaded: distributed-systems, async-processing, integration-resilience, end-to-end-testing, test-engineering, ai-governance
- Constraints: timeout is not business rejection; same idempotency key recovers protocol; human owns commits

## Changes

- Worker tests: lost first Central response then retry restores same protocol; restart resumes POLL without second submit
- `tests/e2e/timeout-recovery.sh` — clear local protocol, recover identity, restart API, await CONFIRMADA
- Central Flyway `V6__recovery_parcel_seeds.sql` (`ben-recover`)
- Acceptance gate checked; SPEC-008 evidence updated

## Validation

```text
$ cd apps/portability-api && ./mvnw -B test -Dtest=CentralPortabilityWorkerTest
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

$ ./tests/e2e/timeout-recovery.sh
original protocol=prot-…
recovered same protocol=prot-…   # identical
OK … -> CONFIRMADA
TIMEOUT_RECOVERY_E2E_PASS
```

- Validation: PASS
- Human review / decision: pending
- Commit reference: pending (user-executed)
- Remaining risks: none for this gate; optional future work remains tracing dashboard / production auth
