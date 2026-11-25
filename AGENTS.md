# Engineering agent contract

Read the relevant spec, ADR and OpenAPI contract before editing. Select only relevant skills from `.agents/skills/` (Claude Code discovery may require explicit configuration). Treat source requirements as authoritative; flag ambiguities and document proposed decisions. Do not invent test results.

## Rules
- Domain logic independent of Spring/JPA/HTTP; package by feature; adapters implement ports.
- Monetary values use BigDecimal/NUMERIC, never binary floating point. Dates use LocalDate, event time UTC Instant.
- Create operation, reservation, audit event and job atomically. Never hold database transactions during HTTP calls.
- Stable scoped idempotency keys, canonical payload hash, uniqueness constraints in both services; different payload with same key returns 409.
- Confirmed/rejected are terminal. Timeout is not rejection. Update receiving account only on verified confirmation.
- Append audit events with sequence and correlation ID; redact secrets and bank account details.
- Verify HMAC before trusting messages; use fresh nonce for retries but unchanged idempotency key.
- No unrelated edits, secrets, real PII, unapproved dependencies or weakened tests.

## Workflow
Spec → design/ADR → task → implementation → tests → independent review → human approval → evidence. Record actual prompts, commands, outcomes and unresolved risks in `ai/sessions/`. If validation cannot run, explicitly mark NOT_RUN.

## Done
Acceptance criteria mapped to tests; contracts and docs updated; failures and concurrency covered; actual command evidence recorded. Human approval required for security or financial invariant changes.
