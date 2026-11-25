---
name: message-integrity
description: Sign and verify HTTP messages. Use when the task touches this engineering concern.
---
# message-integrity

## Objective
Sign and verify HTTP messages.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: HMAC-SHA256 over canonical method/path/query/body hash/timestamp/nonce/key and correlation; bind responses to request; constant-time verify; replay cache.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Tamper, replay, expired timestamp and known-vector tests. Do not claim success without execution evidence.
