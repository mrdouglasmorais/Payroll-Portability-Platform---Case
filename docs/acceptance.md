# Acceptance gates

Mapped to executable evidence. Unchecked items remain open or only partially covered.

- [x] Consent and scoped agenda, valid installment selection and destination.  
  Evidence: consent + per-agreement agenda filter (`SyncAgendaService`); discount-date eligibility in `CreatePortabilityService`; `PortabilityApiWebTest`
- [x] Central 202 protocol, delayed status query, deterministic success and rejection.  
  Evidence: M3 `CentralHmacIntegrationTest`; dual-stack confirm/reject; Compose CI smoke
- [x] Receiving account changes only on confirmation; rejection preserves original.  
  Evidence: platform `effective_*` + worker confirm/reject asserts; Central `V7__effective_receiving_account` applied on confirm only
- [ ] Same key same identity, mismatch 409, concurrent duplicate and competing installments tested on PostgreSQL.  
  Evidence: atomic claim on platform + Central (`ON CONFLICT` + `FOR UPDATE`); concurrent HTTP same-response load proof still desired for M10
- [x] Acceptance followed by timeout recovers original protocol after retry/restart.  
  Evidence: lost-response + restart tests; `expiredRunningLeaseIsReclaimedByAnotherWorker`; `staleFenceDoesNotMutateFinancialState` with transactional fence lock + rollback
- [x] HMAC request/response tampering, expired timestamp and replay rejected.  
  Evidence: M3 `CentralHmacIntegrationTest`, `HmacCanonicalTest`
- [x] 503, timeout, malformed response and exhausted retries visible without false rejection.  
  Evidence: typed `last_error_code`; GET `processing` + `centralProtocol`; `INTEGRATION_FAILURE` only when fence owned and non-terminal; runbook `docs/runbooks/reconciliation.md`
- [x] Append-only audit, atomic state/event changes and ordered timeline.  
  Evidence: create atomic tests; dual-stack `/eventos`; fenced event append
- [ ] React end-to-end flows and accessibility checks.  
  Evidence: Vitest + Playwright local/Compose; Playwright not yet a CI job (M10)
- [ ] Clean checkout instructions and actual CI/test evidence; documented trade-offs and future work.  
  Evidence: Maven/frontend/security/compose CI; OpenAPI Redocly gate fixed (M9). k6/Playwright process-kill still optional
- [x] AI prompts, constraints, results and human review recorded; no secrets or real data.  
  Evidence: `ai/sessions/AI-000` … `AI-009`

Optional: M10 fault injection / metrics / Playwright-in-CI; M11 architecture defense docs depth.
