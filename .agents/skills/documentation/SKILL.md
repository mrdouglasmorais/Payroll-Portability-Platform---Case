---
name: documentation
description: Maintain useful technical docs. Use when the task touches this engineering concern.
---
# documentation

## Objective
Maintain useful technical docs.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Document actual commands, decisions, trade-offs and limitations; never claim planned features exist.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
README verified on clean checkout. Do not claim success without execution evidence.
