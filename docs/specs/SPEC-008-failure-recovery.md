# SPEC-008-failure-recovery

Status: Proposed

Source mapping: RF6/RNF3/RNF4. Identifiers assigned locally, not in original challenge.

## Behavior
Bound retries with persisted next_run_at, backoff and jitter; honor Retry-After; lease and fence worker; exhausted or contradictory response requires review without business rejection.

## Acceptance
Timeout, 503, malformed response, invalid signature and worker crash are distinguishable; reservations stay held for ambiguous outcome.

## Evidence
Pending implementation and tests.
