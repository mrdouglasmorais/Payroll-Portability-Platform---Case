---
name: spring-boot-engineering
description: Configure Spring adapters. Use when the task touches this engineering concern.
---
# spring-boot-engineering

## Objective
Configure Spring adapters.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Use constructor injection, validated configuration and explicit transaction boundaries.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Context and adapter tests. Do not claim success without execution evidence.
