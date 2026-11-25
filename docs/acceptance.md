# Acceptance gates — all pending

- [ ] Consent and scoped agenda, valid installment selection and destination.
- [ ] Central 202 protocol, delayed status query, deterministic success and rejection.
- [ ] Receiving account changes only on confirmation; rejection preserves original.
- [ ] Same key same identity, mismatch 409, concurrent duplicate and competing installments tested on PostgreSQL.
- [ ] Acceptance followed by timeout recovers original protocol after retry/restart.
- [ ] HMAC request/response tampering, expired timestamp and replay rejected.
- [ ] 503, timeout, malformed response and exhausted retries visible without false rejection.
- [ ] Append-only audit, atomic state/event changes and ordered timeline.
- [ ] React end-to-end flows and accessibility checks.
- [ ] Clean checkout instructions and actual CI/test evidence; documented trade-offs and future work.
- [ ] AI prompts, constraints, results and human review recorded; no secrets or real data.

Optional enhancements: tracing dashboard, k6, additional security scanners. Do not delay core requirements for these.
