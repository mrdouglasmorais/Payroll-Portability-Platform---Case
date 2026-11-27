# SPEC-005-idempotency

Status: Accepted

Source mapping: RNF2. Identifiers assigned locally, not in original challenge.

## Behavior
Both services persist scoped idempotency keys. **Platform** uses a semantic fingerprint (`RequestHash`: sorted parcelas + account fields). **Central** hashes the **raw request body bytes** (HMAC-verified payload). Same key + same fingerprint/bytes → original identity; mismatch → 409. Exclude nonce and signature from the platform hash. Detail: [ADR-005](../adr/ADR-005-idempotency.md).

## Acceptance
Concurrent equal requests produce one operation and one protocol; different keys cannot double-port same installment.

## Evidence
Platform: `CreatePortabilityServiceTest`, `PortabilityApiWebTest` (409 on payload mismatch). Central: M3 `CentralHmacIntegrationTest`.
