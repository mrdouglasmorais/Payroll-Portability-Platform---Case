# Acceptance gates

Mapped to executable evidence. Unchecked items remain open or only partially covered.

- [x] Consent and scoped agenda, valid installment selection and destination.  
  Evidence: `CentralPortabilityWorkerTest`, `PortabilityApiWebTest`, `./tests/e2e/dual-stack.sh`, Playwright `operator-flow.spec.ts`
- [x] Central 202 protocol, delayed status query, deterministic success and rejection.  
  Evidence: M3 `CentralHmacIntegrationTest`; M6 dual-stack confirm + reject paths
- [x] Receiving account changes only on confirmation; rejection preserves original.  
  Evidence: worker confirm consumes reservation / reject releases (`CentralPortabilityWorkerTest`); UI masks account until terminal
- [x] Same key same identity, mismatch 409, concurrent duplicate and competing installments tested on PostgreSQL.  
  Evidence: `CreatePortabilityServiceTest`, dual-stack idempotent replay + 409
- [ ] Acceptance followed by timeout recovers original protocol after retry/restart.  
  Gap: dedicated lost-response restart harness not yet automated in Compose E2E
- [x] HMAC request/response tampering, expired timestamp and replay rejected.  
  Evidence: M3 `CentralHmacIntegrationTest`, `HmacCanonicalTest`
- [x] 503, timeout, malformed response and exhausted retries visible without false rejection.  
  Evidence: `CentralPortabilityWorkerTest.transientFailureDoesNotRejectBusiness`
- [x] Append-only audit, atomic state/event changes and ordered timeline.  
  Evidence: create atomic job/event tests; dual-stack `/eventos` sequence check; UI timeline
- [x] React end-to-end flows and accessibility checks.  
  Evidence: Vitest wizard tests; Playwright operator flow (live Compose)
- [x] Clean checkout instructions and actual CI/test evidence; documented trade-offs and future work.  
  Evidence: README, CI unit/integration jobs, `ai/sessions/AI-006-*.md`, Miro system design
- [x] AI prompts, constraints, results and human review recorded; no secrets or real data.  
  Evidence: `ai/sessions/AI-000` … `AI-006`

Optional enhancements: tracing dashboard, additional security scanners. k6 lab smoke recorded in AI-006 (not a production SLO claim).
