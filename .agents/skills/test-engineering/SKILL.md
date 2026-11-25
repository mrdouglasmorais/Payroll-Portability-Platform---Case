---
name: test-engineering
description: Design behavior-focused tests. Use when the task touches this engineering concern.
---
# test-engineering

## Objective
Design behavior-focused tests.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Cover invariants, error paths and boundary conditions; deterministic clocks; do not weaken assertions.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Test matrix and actual reports. Do not claim success without execution evidence.
