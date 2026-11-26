# AI-013 — Observability, request hardening, manual validation

- Task / spec / ADR: ADR-014 observability; application security hardening; acceptance DONE
- Tool and model: Cursor agent (Composer)
- Skills loaded: observability-engineering, application-security, test-engineering, end-to-end-testing, documentation, ai-governance, devsecops
- Actual prompt (redacted): close residual engineering gaps; run manual scenarios; mark acceptance DONE. Do not publish internal evaluation scores in repository artifacts.
- Constraints: no invented evidence; no secrets; keep metrics low-cardinality; human owns commits

## Changes

- Micrometer Prometheus registry on platform + Central
- `PortabilityTelemetry` counters/gauges (operations, jobs, central failure class, backlog)
- `CorrelationIdFilter` (MDC + response header)
- `SecurityHeadersFilter` + restricted CORS
- Request bounds (idempotency key, parcels, account fields)
- Milestone `M10`; docs for observability; Playwright selects any OPEN parcel
- Acceptance marked DONE for evidenced local gates

## Commands and actual outputs

```text
$ apps/portability-api ./mvnw -B clean verify
Tests run: 37, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS

$ apps/central-simulator ./mvnw -B clean verify
Tests run: 9, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS

$ apps/web pnpm test
Tests 9 passed (9)

$ docker compose up --build -d
# healthy

$ curl -sI http://localhost:8080/api/v1/status
# X-Correlation-Id, X-Content-Type-Options, X-Frame-Options, CSP present
# milestone M10

$ curl -sf http://localhost:8080/actuator/prometheus | rg portability_
# portability_operations_total / backlog metrics present

$ ./tests/e2e/dual-stack.sh
DUAL_STACK_E2E_PASS

$ ./tests/e2e/timeout-recovery.sh
TIMEOUT_RECOVERY_E2E_PASS

$ # Scenario D: same Idempotency-Key → same operationId; payload mismatch → 409
SCENARIO_D_PASS

$ cd tests/e2e && pnpm test   # after lab-reset; not parallel with API restart
1 passed
```

## Follow-up (same session family)

- Lab-reset refuses PORTED reopen; clean lab = `compose down -v`
- HTTP concurrent same-key MockMvc test added
- `process-kill-recovery.sh` (SIGKILL after Central accept) PASS
- Playwright: locator fix + axe + keyboard + mobile viewport — 2 passed
- CI on published `3c82d9` failed on Playwright strict mode; fixed pending push

## Validation

- Validation: PASS (local); CI green pending next push
- Human review / decision: performed (candidate)
- Commit reference: pending (user-executed)
- Remaining risks: confirm Actions on next SHA; operator auth lab-scoped
