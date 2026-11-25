---
name: solution-architecture
description: Design boundaries and ADRs. Use when the task touches this engineering concern.
---
# solution-architecture

## Objective
Design boundaries and ADRs.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Compare modular monolith and microservices; isolate Central; avoid speculative infrastructure.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
C4 context/container and ADR reviewed. Do not claim success without execution evidence.
