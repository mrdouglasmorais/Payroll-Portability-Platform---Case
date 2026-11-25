---
name: java-engineering
description: Implement maintainable Java. Use when the task touches this engineering concern.
---
# java-engineering

## Objective
Implement maintainable Java.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Use explicit types, BigDecimal, immutable values; avoid framework leakage.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Compile and domain tests. Do not claim success without execution evidence.
