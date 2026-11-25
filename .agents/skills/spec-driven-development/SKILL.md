---
name: spec-driven-development
description: Translate requirements into executable acceptance criteria. Use when the task touches this engineering concern.
---
# spec-driven-development

## Objective
Translate requirements into executable acceptance criteria.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Trace each change to a requirement; record assumptions separately.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Spec-to-test mapping. Do not claim success without execution evidence.
