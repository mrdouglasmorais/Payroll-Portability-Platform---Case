# AI-009 — M9 Financial Correctness

## Prompt (human)
Independent M8 review: residual fencing TOCTOU, unfenced failure events, Central effective account gap, OpenAPI CI FAIL, plus P1 agenda/timeouts/Retry-After/typed errors. DoD = qualidade 100% on M9 critical path.

## Skills
`financial-domain`, `idempotency-engineering`, `distributed-systems`, `async-processing`, `api-contract-design`, `devsecops`, `ai-governance`, `documentation`

## Changes
- Fence: `lockOwnedFence` (`SELECT … FOR UPDATE`); confirm/reject throw `StaleFenceException` if job mark fails → txn rollback
- Failure events: `recordIntegrationFailureWithFence` + `appendEventsIfNonTerminal` (no status regression)
- Central: `V7` effective account on confirm; atomic idempotency claim
- Agenda: upsert OPEN snapshots; filter by accepted agreement consent
- HTTP: RestClient connect/read timeouts from `CentralProperties`
- Poll: propagate `Retry-After` on 204; typed `last_error_code`; retries as `FAILED`/`RETRYING`
- OpenAPI: servers/security/summaries + `redocly.yaml`; CI lint uses config
- Runbook: `docs/runbooks/reconciliation.md`

## Validation (actual)
```
$ cd apps/portability-api && ./mvnw -B verify
# Tests run: 27, Failures: 0 — BUILD SUCCESS

$ cd apps/central-simulator && ./mvnw -B verify
# Tests run: 9, Failures: 0 — BUILD SUCCESS

$ npx @redocly/cli@1.34.5 lint --config redocly.yaml contracts/openapi/*.yaml
# Woohoo! Your API descriptions are valid. (2 warnings: localhost servers)

$ cd apps/web && pnpm test
# Tests 6 passed (6)
```

## NOT_RUN
- Concurrent HTTP idempotency load suite (M10)
- Playwright/k6 in CI (M10)
- Real process-kill after Central accept (M10)

## Human review
Pending for financial invariant + Central schema changes.
