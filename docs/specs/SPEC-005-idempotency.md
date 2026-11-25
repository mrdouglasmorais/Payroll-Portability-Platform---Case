# SPEC-005-idempotency

Status: Proposed

Source mapping: RNF2. Identifiers assigned locally, not in original challenge.

## Behavior
Both services persist scoped key, canonical semantic fingerprint and operation/protocol; same payload returns original identity, mismatch 409. Sort installment IDs for canonical set; exclude nonce and signature.

## Acceptance
Concurrent equal requests produce one operation and one protocol; different keys cannot double-port same installment.

## Evidence
Pending implementation and tests.
