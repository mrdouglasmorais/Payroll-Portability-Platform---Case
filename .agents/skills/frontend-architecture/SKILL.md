---
name: frontend-architecture
description: Organize frontend by feature. Use when the task touches this engineering concern.
---
# frontend-architecture

## Objective
Organize frontend by feature.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Separate app shell, features and shared UI; avoid cross-feature internals.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Import boundary review. Do not claim success without execution evidence.
