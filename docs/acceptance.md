# Acceptance gates

Status: **DONE** for local critical gates on M10 (see `ai/sessions/AI-013-observability-hardening.md`).

- [x] Consent and scoped agenda, valid installment selection and destination.  
  Evidence: Playwright operator flow PASS; dual-stack authorize/agenda
- [x] Central 202 protocol, delayed status query, deterministic success and rejection.  
  Evidence: `DUAL_STACK_E2E_PASS`
- [x] Receiving account changes only on confirmation; rejection preserves original.  
  Evidence: worker confirm/reject asserts; Central effective account on confirm only
- [x] Same key same identity, mismatch 409, concurrent duplicate and competing installments tested on PostgreSQL.  
  Evidence: CreatePortabilityServiceTest concurrent suites; manual Scenario D same-key + 409
- [x] Acceptance followed by timeout recovers original protocol after retry/restart.  
  Evidence: `TIMEOUT_RECOVERY_E2E_PASS`
- [x] HMAC request/response tampering, expired timestamp and replay rejected.  
  Evidence: Central HMAC ITs in Maven verify
- [x] 503, timeout, malformed response and exhausted retries visible without false rejection.  
  Evidence: typed codes; worker transient/permanent paths; runbook
- [x] Append-only audit, atomic state/event changes and ordered timeline.  
  Evidence: create atomic; dual-stack `/eventos`; fenced append
- [x] React end-to-end flows and accessibility checks.  
  Evidence: Playwright happy path + keyboard Tab order + `@axe-core/playwright` (wcag2a/aa) on mobile viewport; 2 passed on clean Compose volumes
- [x] Clean checkout instructions and actual CI/test evidence; documented trade-offs and future work.  
  Evidence: README; local suite PASS. Actions on `3c82d9` failed (Playwright locator) — fixed in working tree; re-confirm after push
- [x] AI prompts, constraints, results and human review recorded; no secrets or real data.  
  Evidence: `ai/sessions/AI-000` … `AI-013`; human review performed for financial/release findings

Formal RF/RNF rows: [compliance-matrix.md](compliance-matrix.md). Observability: [architecture/observability.md](architecture/observability.md).
