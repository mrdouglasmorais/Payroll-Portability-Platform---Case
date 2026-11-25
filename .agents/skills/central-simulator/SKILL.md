---
name: central-simulator
description: Implement independent external system. Use when the task touches this engineering concern.
---
# central-simulator

## Objective
Implement independent external system.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Persist protocol and idempotency; deterministic delayed confirmation, rejection, timeout after acceptance; own database.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Fault scenario and replay tests. Do not claim success without execution evidence.
