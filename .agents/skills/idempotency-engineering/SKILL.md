---
name: idempotency-engineering
description: Prevent duplicate financial effects. Use when the task touches this engineering concern.
---
# idempotency-engineering

## Objective
Prevent duplicate financial effects.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Scope key by caller and operation; persist canonical fingerprint; same key same result; mismatch 409; stable external key; concurrent reservation constraint.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Concurrent duplicate, lost response and restart tests. Do not claim success without execution evidence.
