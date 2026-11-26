# Acceptance gates

Status: **DONE** for critical gates on published tip `d0dcbbc` (remote CI SUCCESS 8/8 — [run 45740557504](https://github.com/mrdouglasmorais/Payroll-Portability-Platform---Case/actions/runs/45740557504)). Unpublished working-tree changes must not claim remote CI until a new green Actions run.

- [x] Consent and scoped agenda, valid installment selection and destination.  
  Evidence: Playwright operator flow; dual-stack authorize/agenda
- [x] Central 202 protocol, delayed status query, deterministic success and rejection.  
  Evidence: `DUAL_STACK_E2E_PASS`
- [x] Receiving account changes only on confirmation; rejection preserves original.  
  Evidence: worker confirm/reject asserts; Central effective account on confirm only
- [x] Same key same identity, mismatch 409, concurrent duplicate and competing installments tested on PostgreSQL.  
  Evidence: CreatePortabilityServiceTest concurrent suites; Scenario D same-key + 409
- [x] Acceptance followed by timeout recovers original protocol after retry/restart.  
  Evidence: `TIMEOUT_RECOVERY_E2E_PASS` (lost local protocol + idempotent restore)
- [x] Hard process interrupt after local protocol persist recovers without duplicate ops.  
  Evidence: `PROCESS_KILL_RECOVERY_E2E_PASS` — covers **post-persist** SIGKILL only (see residual below)
- [x] HMAC request/response tampering, expired timestamp and replay rejected.  
  Evidence: Central HMAC ITs in Maven verify / CI `bf0c767`
- [x] 503, timeout, malformed response and exhausted retries visible without false rejection.  
  Evidence: typed codes; worker transient/permanent paths; runbook
- [x] Append-only audit, atomic state/event changes and ordered timeline.  
  Evidence: create atomic; dual-stack `/eventos`; fenced append
- [x] React end-to-end flows and accessibility **smoke** (axe wcag2a/aa + keyboard) across wizard steps on desktop and mobile.  
  Evidence: Playwright — authorization, agenda, selection error, destination, review, status, audit timeline. **Not** a claim of full WCAG 2.2 AA certification.
- [x] Clean checkout instructions and actual CI/test evidence; documented trade-offs and future work.  
  Evidence: README; remote CI SUCCESS on `d0dcbbc` ([run 45740557504](https://github.com/mrdouglasmorais/Payroll-Portability-Platform---Case/actions/runs/45740557504)); local AI-014 gates
- [x] AI prompts, constraints, results and human review recorded; no secrets or real data.  
  Evidence: `ai/sessions/AI-000` … `AI-014`; human review performed for financial/release findings (candidate-owned)

## Explicit residuals (accepted)

- Crash between Central accept and local `central_protocol` persist is not deterministically automated (no production test hooks). Mitigation: same idempotency key + Central replay; `timeout-recovery.sh`; reconciliation runbook.
- Flyway `V8` still runs on clean Central migrate (historical demo reopen). `V9` documents policy and does **not** undo V8. Reset strategy = disposable volumes only.
- Operational Prometheus counters are not authoritative accounting.

Formal RF/RNF rows: [compliance-matrix.md](compliance-matrix.md). Observability: [architecture/observability.md](architecture/observability.md).
