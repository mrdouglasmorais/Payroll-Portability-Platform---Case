---
name: database-engineering
description: Design PostgreSQL persistence. Use when the task touches this engineering concern.
---
# database-engineering

## Objective
Design PostgreSQL persistence.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Use Flyway, constraints, indexes, atomic transactions and measured queries.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Migration and concurrent integration tests. Do not claim success without execution evidence.
