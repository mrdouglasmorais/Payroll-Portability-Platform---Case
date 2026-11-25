---
name: product-design
description: Design operator workflow. Use when the task touches this engineering concern.
---
# product-design

## Objective
Design operator workflow.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Consent → agenda → selection → review → operation history; distinguish rejection from integration issue.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Usability walkthrough. Do not claim success without execution evidence.
