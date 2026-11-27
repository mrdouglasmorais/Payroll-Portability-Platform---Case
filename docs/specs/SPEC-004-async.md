# SPEC-004-async

Status: Accepted

Source mapping: RF4. Identifiers assigned locally, not in original challenge.

## Behavior
Submit to Central outside DB transaction; persist protocol; poll status; 204/Retry-After means pending. Confirm only on verified terminal status; update account atomically with audit.

## Acceptance
Lost response recovered with stable key; restart resumes; rejection preserves prior account.

## Evidence
M4 worker SUBMIT→POLL→finalize: `CentralPortabilityWorkerTest` (confirm, reject, 503 without business rejection).
