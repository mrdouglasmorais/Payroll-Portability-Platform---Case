---
name: distributed-systems
description: Handle partial failures. Use when the task touches this engineering concern.
---
# distributed-systems

## Objective
Handle partial failures.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Assume at-least-once delivery; separate local and remote commits; reconcile ambiguous outcomes.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Crash-window recovery tests. Do not claim success without execution evidence.
