---
name: financial-domain
description: Protect financial consistency. Use when the task touches this engineering concern.
---
# financial-domain

## Objective
Protect financial consistency.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Use exact decimals; never change receiving account before confirmation; preserve original account on rejection.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Money and settlement tests. Do not claim success without execution evidence.
