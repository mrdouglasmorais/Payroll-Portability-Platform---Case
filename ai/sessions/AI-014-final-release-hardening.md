# AI-014 — Final release hardening

- Task / spec / ADR: RNF08 doc honesty; ADR-014 afterCommit metrics; SPEC-009 a11y smoke; RF04 recovery window honesty
- Tool and model: Cursor agent (Composer)
- Skills loaded: ai-governance, documentation, observability-engineering, test-engineering, accessibility, financial-domain
- Actual prompt (redacted): Final Release Hardening — consolidate docs against bf0c767 CI green; afterCommit financial counters; expand axe/keyboard; evaluate pre-persist kill; validate; no commit/push without human auth
- Constraints: no invented evidence; no rewrite of historical Flyway V8; no production test hooks; no WCAG certification claim; no CI claim for unpublished commits; no autopontuação

## Baseline verified before edits

- Branch `main`, HEAD `bf0c76773fc609c97b3977fc8415defa9cd587d9` matched remote baseline
- Remote CI SUCCESS 8/8: https://github.com/mrdouglasmorais/Payroll-Portability-Platform---Case/actions/runs/45738641426

## Changes / files

- Docs: README, compliance-matrix, acceptance, release-readiness-report, observability, overview, worker-lease, idempotency, reconciliation, ADR-014/015, e2e README, ai/README
- Metrics: `PortabilityTelemetry` afterCommit + `PortabilityTelemetryCommitTest` (commit + rollback)
- A11y: Playwright axe+keyboard across auth/agenda/error/destino/revisão/status/auditoria (desktop+mobile)
- Seeds: `V10__additional_ui_parcels.sql` (parcela-ui-3/4) — does not rewrite V8
- Recovery: documented critical window; process-kill script header clarified; no production kill hooks

## Suggestions rejected and why

- Deterministic SIGKILL between Central 202 and `attachProtocol` via production/debug hooks — disproportionate and unsafe for financial service; documented residual instead
- Rewriting or deleting V8 — breaks Flyway checksums on already-applied DBs
- Claiming WCAG 2.2 AA certification from axe smoke — out of scope / dishonest

## Commands and actual outputs

```text
$ git rev-parse HEAD
bf0c76773fc609c97b3977fc8415defa9cd587d9

$ apps/portability-api ./mvnw -B clean verify
Tests run: 40, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
# includes PortabilityTelemetryCommitTest (commit + rollback)

$ apps/central-simulator ./mvnw -B clean verify
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS

$ apps/web pnpm test
Tests 9 passed (9)

$ docker compose down -v && docker compose up --build -d
# healthy stack on clean volumes (V10 applied)

$ ./tests/e2e/dual-stack.sh
DUAL_STACK_E2E_PASS

$ ./tests/e2e/timeout-recovery.sh
TIMEOUT_RECOVERY_E2E_PASS

$ ./tests/e2e/process-kill-recovery.sh
PROCESS_KILL_RECOVERY_E2E_PASS

$ cd tests/e2e && pnpm test
3 passed (11.6s)
# happy path + a11y desktop + a11y mobile

$ ./scripts/lab-reset-demo-parcels.sh
LAB_RESET_REFUSED (exit 2)

$ curl -sf http://localhost:8080/actuator/prometheus | rg portability_operations_total
# HELP ... (after successful commit)
portability_operations_total{...,outcome="created"} 3.0
portability_operations_total{...,outcome="confirmed"} 3.0
```

## Validation

- Validation: PASS (local on AI-014 working tree)
- Remote CI for AI-014 changes: NOT_RUN (unpublished; do not claim)
- Human review / decision: pending human commit/push authorization
- Commit reference: pending (user-executed)
- Remaining risks:
  - Pre-persist Central-accept window not deterministically process-killed
  - V8 still executes on clean Central migrate; V9 does not undo
  - Metrics remain operational/non-accounting
  - A11y is smoke, not WCAG certification
  - Confirm Actions after human publishes AI-014 SHA
