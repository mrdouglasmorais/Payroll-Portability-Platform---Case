# Acceptance gates

Mapped to executable evidence. Unchecked items remain open or only partially covered.
Reopened for M8 after independent review (financial integrity / concurrency / ops visibility).

- [x] Consent and scoped agenda, valid installment selection and destination.  
  Evidence: `CentralPortabilityWorkerTest`, `PortabilityApiWebTest`, `./tests/e2e/dual-stack.sh`, Playwright `operator-flow.spec.ts`
- [x] Central 202 protocol, delayed status query, deterministic success and rejection.  
  Evidence: M3 `CentralHmacIntegrationTest`; M6 dual-stack confirm + reject paths
- [ ] Receiving account changes only on confirmation; rejection preserves original.  
  Evidence (M8 in progress): installment `effective_*` columns + `confirmPathConsumesReservation` /
  `rejectPathReleasesReservation` assert before/after. Central simulator still stores requested
  account on protocol create — production-grade “conta vigente” there is follow-up.
- [ ] Same key same identity, mismatch 409, concurrent duplicate and competing installments tested on PostgreSQL.  
  Evidence: `CreatePortabilityServiceTest`, dual-stack idempotent replay + 409; M8 atomic
  `claimIdempotencyOrFind` — concurrent HTTP same-response proof still to harden in CI.
- [ ] Acceptance followed by timeout recovers original protocol after retry/restart.  
  Evidence: `lostAcceptanceResponseRecoversSameProtocolOnRetry`, `restartResumesPollWithoutResubmitting`,
  `expiredRunningLeaseIsReclaimedByAnotherWorker`, `staleFenceDoesNotMutateFinancialState`.
  Full process-kill fault injection after Central accept remains open.
- [x] HMAC request/response tampering, expired timestamp and replay rejected.  
  Evidence: M3 `CentralHmacIntegrationTest`, `HmacCanonicalTest`
- [ ] 503, timeout, malformed response and exhausted retries visible without false rejection.  
  Evidence: `transientFailureDoesNotRejectBusiness`; M8 GET `processing.requiresAction` +
  `INTEGRATION_FAILURE` timeline events. Operator UX polish still partial.
- [x] Append-only audit, atomic state/event changes and ordered timeline.  
  Evidence: create atomic job/event tests; dual-stack `/eventos` sequence check; UI timeline
- [x] React end-to-end flows and accessibility checks.  
  Evidence: Vitest wizard tests; Playwright operator flow (live Compose)
- [ ] Clean checkout instructions and actual CI/test evidence; documented trade-offs and future work.  
  Evidence: README, CI unit/integration/security jobs; Compose/Playwright/k6 still local /
  AI-006/007 evidence — CI smoke gate pending.
- [x] AI prompts, constraints, results and human review recorded; no secrets or real data.  
  Evidence: `ai/sessions/AI-000` … `AI-008`

Optional enhancements: tracing dashboard, additional security scanners. k6 lab smoke recorded in AI-006 (not a production SLO claim). Lost-response/restart recovery recorded in AI-007. M8 financial integrity hardening in AI-008.
