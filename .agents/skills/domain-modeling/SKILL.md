---
name: domain-modeling
description: Model consent, schedule and portability. Use when the task touches this engineering concern.
---
# domain-modeling

## Objective
Model consent, schedule and portability.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Enforce eligibility and terminal states inside aggregates; distinguish technical status.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Domain invariant tests. Do not claim success without execution evidence.
