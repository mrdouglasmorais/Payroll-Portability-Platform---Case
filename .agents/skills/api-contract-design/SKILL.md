---
name: api-contract-design
description: Maintain REST contracts. Use when the task touches this engineering concern.
---
# api-contract-design

## Objective
Maintain REST contracts.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Use OpenAPI, explicit status codes, RFC 9457 errors; never expose persistence entities.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Contract validation and negative tests. Do not claim success without execution evidence.
