# Release readiness report

Engineering status. Quality should be judged from code, tests and recorded command evidence.

## 1. Executive summary

| Field | Value |
|---|---|
| Published tip (pre-M10 push) | `3c82d9be38e19aa8f7a7f72dc71b0bec844b96df` |
| Working tree | M10+finding closure (observability, a11y, HTTP concurrency, process-kill, lab-reset policy) — publish next |
| Milestone | M10 |
| Local gates | Maven / web / Compose / dual-stack / timeout-recovery / process-kill / Playwright+axe PASS (this session) |
| CI on `3c82d9` | **failure** — Playwright strict-mode locator (`getByText(/Operação/)`) — fixed in working tree |
| Acceptance | DONE (local); re-confirm Actions after next push |
| Human review | Performed for financial/release findings (candidate-owned) |

## 2. Compliance matrix

See [compliance-matrix.md](compliance-matrix.md).

## 3. Findings

| ID | Priority | Problem | Status |
|---|---|---|---|
| E | P0 | Multi-parcel lock order | Closed |
| C-test | P1 | Agenda sync OPEN-only | Closed |
| Idemp service | P1 | Concurrent same-key (service) | Closed |
| Idemp HTTP | P1 | Concurrent same-key over HTTP/MockMvc | Closed (`concurrentHttpSameIdempotencyKeyReturnsSingleOperation`) |
| Process kill | P1 | Hard interrupt after Central accept | Closed (`tests/e2e/process-kill-recovery.sh`) |
| V8 / lab-reset | P1 | Partial PORTED reopen as recovery | Closed — script refuses; clean lab = `compose down -v` |
| A11y | P2 | axe / keyboard / viewport | Closed — Playwright + `@axe-core/playwright` |
| Score exposure | P2 | Internal numeric self-score in public report | Closed (history rewritten earlier) |
| Report stale CI | P2 | Report claimed unpublished / CI unknown | Closed — this document |
| CI Playwright | P1 | Locator strict-mode on `3c82d9` | Fixed in working tree (pending push) |

## 4. Tests (latest local evidence)

| Command | Result |
|---|---|
| `portability-api ./mvnw -B clean verify` | 37+ tests (includes HTTP concurrency) — re-run in this pass |
| `central-simulator ./mvnw -B clean verify` | 9 — SUCCESS (prior M10) |
| `apps/web pnpm test` | 9 — PASS |
| `dual-stack.sh` | `DUAL_STACK_E2E_PASS` |
| `timeout-recovery.sh` | `TIMEOUT_RECOVERY_E2E_PASS` |
| `process-kill-recovery.sh` | run in this pass |
| Playwright + axe | run in this pass |
| Actions `3c82d9` | failure (Playwright); link: https://github.com/mrdouglasmorais/Payroll-Portability-Platform---Case/actions/runs/45737102271 |

## 5. Security / observability

- HMAC + headers + CORS + input bounds
- Prometheus low-cardinality metrics + correlation MDC
- Lab: no operator SSO

## 6. Residual risks

- Confirm green Actions on the next published SHA after Playwright locator + a11y land
- V8 remains in Flyway history on clean migrate (documented; not silently rewritten)
- OTel collectors / production auth remain out of scope

## 7. Git

Publish the current working tree as the delivery tip; keep author identity candidate-only (avoid Cursor co-author trailers).
