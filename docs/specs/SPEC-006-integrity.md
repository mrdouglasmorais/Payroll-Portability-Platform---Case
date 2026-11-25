# SPEC-006-integrity

Status: Proposed

Source mapping: RNF1. Identifiers assigned locally, not in original challenge.

## Behavior
Sign request and response using HMAC-SHA256; canonical UTF-8 format documented before coding; cover raw body hash, method, path/query, timestamp, nonce, key ID, correlation and idempotency key; responses bind request nonce and status.

## Acceptance
Reject altered body, invalid signature, expired timestamp and replay; fresh retry nonce preserves key.

## Evidence
Pending implementation and tests.
