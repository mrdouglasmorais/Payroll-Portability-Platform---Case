# AI-012 — Release readiness audit & hardening

- Task / spec / ADR: full RF/RNF audit vs PDF; Finding E lock order; acceptance gates; README; CI Playwright
- Tool and model: Cursor agent (Composer)
- Skills loaded: ai-governance, spec-driven-development, financial-domain, idempotency-engineering, distributed-systems, java-engineering, spring-boot-engineering, test-engineering, devsecops, documentation, product-design, react-engineering, code-review
- Actual prompt (redacted): release-readiness audit and hardening plan (lock order, acceptance evidence, README, CI Playwright)
- Constraints: no invented evidence; no commit/push without human auth; do not rewrite Flyway V8 checksum; no PDF in public repo; prioritize P0/P1

## Baseline

- Branch: `main`
- HEAD at audit start: `7e71031`
- Dirty at start: PT-BR in `Consent.java` / `HttpCentralGateway.java`; untracked `AI-011-ui-ptbr.md`
- PDF: local Downloads only (not published)

## Findings addressed

| ID | Priority | Problem | Correction | Status |
|---|---|---|---|---|
| E | P0 | Multi-installment reserve order non-deterministic | Sort before reserve (platform + Central `TreeSet`); SQL `ORDER BY` | Fixed + tested |
| C-test | P1 | Agenda sync OPEN-only untested | `JdbcInstallmentRepositoryAgendaSyncTest` | Fixed + tested |
| Idemp-concurrent | P1 | Same-key concurrent thin | `concurrentSameIdempotencyKeyReturnsSingleOperation` | Fixed + tested |
| README | P1 | PDF p.4 gaps | README rewrite | Done |
| CI | P1 | Playwright/UI not gated | compose web health + Playwright + lab-reset | Done (remote NOT_RUN) |
| Playwright flake | P1 | Ambiguous Autorização / Confirmada locators | Spec hardened | PASS local |
| Docs | P1 | ai/README stale; no matrix | matrix + session index | Done |

## Suggestions rejected

- Rewrite/delete V8 migration: breaks Flyway checksums
- Full axe CI / process-kill chaos: P2 defer
- Commit/push without human authorization

## Changes / files

- `CreatePortabilityService.java`, `JdbcInstallmentRepository.java`, `CentralProtocolService.java`
- Tests: `CreatePortabilityServiceTest`, `JdbcInstallmentRepositoryAgendaSyncTest`
- `.github/workflows/ci.yml`, `README.md`, `docs/*`, `ai/*`, Playwright spec
- PT-BR domain/gateway messages (AI-011 residual)

## Commands and actual outputs

```text
$ cd apps/portability-api && ./mvnw -B clean verify
Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

$ cd apps/central-simulator && ./mvnw -B clean verify
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

$ cd apps/web && pnpm test && pnpm build
Test Files  5 passed (5)
Tests  9 passed (9)
✓ built

$ npx @redocly/cli@1.34.5 lint --config redocly.yaml contracts/openapi/*.yaml
Woohoo! Your API descriptions are valid. (2 localhost warnings)

$ docker compose up --build -d
# healthy 8080/8081/5173

$ ./tests/e2e/dual-stack.sh
DUAL_STACK_E2E_PASS

$ ./tests/e2e/timeout-recovery.sh
TIMEOUT_RECOVERY_E2E_PASS

$ cd tests/e2e && pnpm test   # after lab-reset; not in parallel with API restart
1 passed (4.3s)
```

Note: first Playwright attempt failed because it ran concurrently with `timeout-recovery` API restart (502). Not a product defect. Locators also tightened.

## Independent review notes

- Fence/account/idempotency paths unchanged except lock ordering — no new financial side effects observed in tests.
- V8 remains historical hazard on clean migrate; documented; lab-reset used.
- Remote CI for delivery SHA: NOT_RUN until push.
- macOS Finder `* 2.class` duplicates in `target/` can break Spring Boot repackage — `mvnw clean` required (local env hygiene).

## Validation

- Validation: PASS (local gates above); CI remote NOT_RUN; human review pending
- Human review / decision: pending
- Commit reference: pending (user-executed)
- Remaining risks: push required for Actions green; axe P2; process-kill P2; no operator auth (lab)
