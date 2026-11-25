---
name: audit-engineering
description: Preserve operation history. Use when the task touches this engineering concern.
---
# audit-engineering

## Objective
Preserve operation history.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Append-only events in same transaction as state change; sequence uniqueness; redact payload; prohibit update/delete via runtime role.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Audit atomicity and ordering tests. Do not claim success without execution evidence.
