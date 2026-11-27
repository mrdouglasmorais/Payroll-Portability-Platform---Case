# ADR-005: idempotency

Status: Accepted

## Context
Technical challenge requires end-to-end idempotency across platform and Central without duplicate financial effects.

## Decision
- **Platform:** scoped persistent idempotency key + **semantic fingerprint** (`RequestHash.forPortability`: beneficiary, sorted parcelas, creditor, bank/agency/account). Same key + same fingerprint → replay; same key + different fingerprint → HTTP 409.
- **Central:** scoped persistent key + fingerprint of the **raw HTTP body bytes** (`SHA-256` of the request body as received). Same key + same bytes → replay; mismatch → 409.

These are intentionally different: the platform owns canonical business fields; the Central trusts the exact bytes it verified under HMAC.

## Alternative
- Semantic fingerprint in both services (rejected for Central): would require the Central to re-serialize JSON and could diverge from the HMAC-verified body.
- In-memory dedupe (rejected): does not survive restart.

## Consequences
- Survives restart; retention policy needed.
- Central is sensitive to JSON key order / whitespace: **safe because the sole emitter is the platform**, which uses a single Jackson serialization path for retries (unchanged body + same Idempotency-Key + fresh nonce).
- README and OpenAPI must describe this split explicitly (see [contract-divergences.md](../architecture/contract-divergences.md)).

## Revisit
If a second emitter appears, move Central to a documented canonical JSON form shared with HMAC body hashing.
