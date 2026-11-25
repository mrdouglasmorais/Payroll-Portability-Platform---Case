---
name: integration-testing
description: Validate real persistence and HTTP. Use when the task touches this engineering concern.
---
# integration-testing

## Objective
Validate real persistence and HTTP.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Use Testcontainers PostgreSQL and WireMock; verify constraints and partial failures.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Integration suite. Do not claim success without execution evidence.
