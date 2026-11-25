# SPEC-004-async

Status: Proposed

Source mapping: RF4. Identifiers assigned locally, not in original challenge.

## Behavior
Submit to Central outside DB transaction; persist protocol; poll status; 204/Retry-After means pending. Confirm only on verified terminal status; update account atomically with audit.

## Acceptance
Lost response recovered with stable key; restart resumes; rejection preserves prior account.

## Evidence
Pending implementation and tests.
