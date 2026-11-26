# Acceptance gates

Mapped to executable evidence in `ai/sessions/AI-012-release-readiness.md`.

- [x] Consent and scoped agenda, valid installment selection and destination.  
  Evidence: consent + per-agreement agenda filter; Playwright operator flow PASS (AI-012)
- [x] Central 202 protocol, delayed status query, deterministic success and rejection.  
  Evidence: `CentralHmacIntegrationTest`; `DUAL_STACK_E2E_PASS` (AI-012)
- [x] Receiving account changes only on confirmation; rejection preserves original.  
  Evidence: worker confirm/reject asserts; Central V7 effective account on confirm only
- [x] Same key same identity, mismatch 409, concurrent duplicate and competing installments tested on PostgreSQL.  
  Evidence: `concurrentSameIdempotencyKeyReturnsSingleOperation`; `concurrentReservationsAllowOnlyOneWinner`; dual-stack 409; Maven verify 34/0 (AI-012)
- [x] Acceptance followed by timeout recovers original protocol after retry/restart.  
  Evidence: `TIMEOUT_RECOVERY_E2E_PASS` (AI-012)
- [x] HMAC request/response tampering, expired timestamp and replay rejected.  
  Evidence: Central HMAC ITs in `./mvnw verify` (AI-012)
- [x] 503, timeout, malformed response and exhausted retries visible without false rejection.  
  Evidence: typed codes; worker transient tests; runbook
- [x] Append-only audit, atomic state/event changes and ordered timeline.  
  Evidence: create atomic; dual-stack `/eventos`; fenced append
- [x] React end-to-end flows and accessibility checks.  
  Evidence: Playwright PASS local (AI-012); markup a11y present. **axe/keyboard CI still P2 open.** Playwright CI job added — remote green requires push.
- [ ] Clean checkout instructions and actual CI/test evidence; documented trade-offs and future work.  
  Evidence: README rewritten (PDF p.4). Local compose/verify PASS. **GitHub Actions on delivery SHA NOT_RUN until human push** (`gh` Forbidden / branch ahead).
- [x] AI prompts, constraints, results and human review recorded; no secrets or real data.  
  Evidence: `ai/sessions/AI-000` … `AI-012` (human review still pending signature)

Formal RF/RNF rows: [compliance-matrix.md](compliance-matrix.md).
