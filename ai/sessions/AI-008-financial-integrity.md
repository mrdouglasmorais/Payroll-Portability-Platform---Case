# AI-008 — M8 Financial Integrity, Concurrency & Operational Hardening

## Prompt (human)
Independent review of AI-006/AI-007 evidence with P0/P1 findings and recommended M8 backlog (RUNNING lease recovery, fencing+financial txn, effective receiving account, ops visibility, concurrent idempotency, backoff/jitter, Retry-After, CI gates).

## Skills applied
`ai-governance`, `financial-domain`, `idempotency-engineering`, `distributed-systems`, `async-processing`, `state-machine`, `api-contract-design`, `test-engineering`, `devsecops`

## Changes
- Flyway `V3__financial_integrity.sql`: `effective_*` on installments; claim index includes `RUNNING`
- `claimNext` recovers `RUNNING` with expired lease; `stillOwns` / `markSucceededIfOwned`
- `FinalizeOperationService`: confirm/reject/attachProtocol gated by fence in same DB txn; optimistic `saveOutcomeIfPending`
- Effective account applied only on confirm; reject asserts unchanged
- GET status returns `processing` (`REQUIRES_REVIEW` when EXHAUSTED) + sanitized `INTEGRATION_FAILURE` events
- Atomic idempotency claim (`ON CONFLICT DO NOTHING` + `FOR UPDATE` wait for bind)
- Exponential backoff + jitter; honor `Retry-After`; permanent vs transient Central errors; sanitized exception messages
- OpenAPI `platform.yaml` + UI processing banner; CI `openapi-contracts` + `compose-smoke`

## Validation (actual)
```
$ cd apps/portability-api && ./mvnw -B verify
# Tests run: 27, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS

$ cd apps/web && pnpm test
# Test Files 4 passed (4); Tests 6 passed (6)
```

Focused worker suite includes `expiredRunningLeaseIsReclaimedByAnotherWorker` and `staleFenceDoesNotMutateFinancialState`.

## NOT_RUN in this session
- Full `docker compose` E2E / Playwright / k6 (CI job added; not executed locally here)
- Process-kill fault injection after Central accept (still simulated via lease SQL + fence)
- Central-simulator “conta vigente” schema parity
- Concurrent HTTP same-response load proof beyond unit/service tests

## Acceptance
Reopened related gates in `docs/acceptance.md` until remaining evidence is recorded.

## Human review
Pending commit/approval for financial invariant changes (fencing + effective account).
