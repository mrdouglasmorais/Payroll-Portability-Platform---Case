# Release readiness report

Engineering status. Quality should be judged from code, tests and recorded command evidence.

## 1. Executive summary

| Field | Value |
|---|---|
| Published tip (CI green) | `bf0c76773fc609c97b3977fc8415defa9cd587d9` |
| Remote CI | [SUCCESS 8/8](https://github.com/mrdouglasmorais/Payroll-Portability-Platform---Case/actions/runs/45738641426) on `bf0c767` |
| Working tree | AI-014 final release hardening (docs + afterCommit metrics + a11y expansion) — **unpublished**; no CI claim until next green Actions |
| Milestone | M10 + AI-014 hardening |
| Local gates | PASS — Maven 40+9, Vitest 9, dual-stack, timeout-recovery, process-kill, Playwright 3 (AI-014) |
| Acceptance | DONE on `bf0c767` + local AI-014 residuals closed/documented |
| Human review | Performed for financial/release findings (candidate-owned) |

## 2. Compliance matrix

See [compliance-matrix.md](compliance-matrix.md).

## 3. Findings

| ID | Priority | Problem | Status |
|---|---|---|---|
| E | P0 | Multi-parcel lock order | Closed |
| C-test | P1 | Agenda sync OPEN-only | Closed |
| Idemp service | P1 | Concurrent same-key (service) | Closed |
| Idemp HTTP | P1 | Concurrent same-key over HTTP/MockMvc | Closed |
| Process kill (post-persist) | P1 | Hard interrupt after local protocol | Closed (`process-kill-recovery.sh`) |
| Process kill (pre-persist window) | P1 | Central accept before local protocol | **Accepted residual** — documented; mitigated by idempotency + timeout-recovery + runbook; no production test hooks |
| V8 / lab-reset | P1 | Partial PORTED reopen as recovery | Script refuses; V8 remains historical on clean migrate; V9 ≠ undo; disposable volumes only |
| Metrics pre-commit | P2 | Counters inside TX | Closed — `afterCommit` + commit/rollback IT |
| A11y scope | P2 | First-step only | Closed as **smoke** across steps (desktop+mobile); not WCAG certification |
| Docs drift (RNF08/CI/axe) | P2 | Stale matrix/README/report | Closed in AI-014 docs pass |
| Score exposure | P2 | Internal numeric self-score | Closed earlier |

## 4. Tests

### Remote (published SHA)

| Gate | Result |
|---|---|
| GitHub Actions `bf0c767` | SUCCESS 8/8 — [run 45738641426](https://github.com/mrdouglasmorais/Payroll-Portability-Platform---Case/actions/runs/45738641426) |

### Local (AI-014 working tree — actual)

| Command | Result |
|---|---|
| `portability-api ./mvnw -B clean verify` | Tests run: 40 — BUILD SUCCESS |
| `central-simulator ./mvnw -B clean verify` | Tests run: 9 — BUILD SUCCESS |
| `apps/web pnpm test` | 9 passed |
| `dual-stack.sh` | `DUAL_STACK_E2E_PASS` |
| `timeout-recovery.sh` | `TIMEOUT_RECOVERY_E2E_PASS` |
| `process-kill-recovery.sh` | `PROCESS_KILL_RECOVERY_E2E_PASS` |
| Playwright a11y+flow | 3 passed |
| Session detail | `ai/sessions/AI-014-final-release-hardening.md` |

## 5. Security / observability

- HMAC + headers + CORS + input bounds
- Prometheus low-cardinality metrics (financial counters after commit) + correlation MDC
- Metrics are operational only — DB + audit trail are authoritative
- Lab: no operator SSO

## 6. Residual risks (accepted)

- Critical window Central-accept → local protocol persist not deterministically killed in CI
- `V8` demo reopen remains executable on **clean** Central Flyway history; unsafe framing for persistent financial upgrades; prefer exclusive seeds + `compose down -v` for disposable labs
- Operator auth, soak, production secret management out of scope
- CI green for AI-014 changes only after human push + Actions

## 7. Git

Do not commit/push without human authorization. Keep author identity candidate-only.
