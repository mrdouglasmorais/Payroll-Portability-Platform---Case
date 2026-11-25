---
name: state-machine
description: Enforce operation transitions. Use when the task touches this engineering concern.
---
# state-machine

## Objective
Enforce operation transitions.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Allow PENDENTE to CONFIRMADA or REJEITADA only; duplicate final status is no-op; conflicting final status triggers review.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Transition matrix tests. Do not claim success without execution evidence.
