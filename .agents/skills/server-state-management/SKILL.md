---
name: server-state-management
description: Manage server data. Use when the task touches this engineering concern.
---
# server-state-management

## Objective
Manage server data.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: TanStack Query for remote state; stop polling terminal operations; reuse submission key on retry; invalidate affected queries.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Polling and mutation tests. Do not claim success without execution evidence.
