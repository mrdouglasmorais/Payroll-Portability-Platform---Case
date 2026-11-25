---
name: performance-engineering
description: Measure concurrency and latency. Use when the task touches this engineering concern.
---
# performance-engineering

## Objective
Measure concurrency and latency.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Test duplicate keys and competing reservations; report environment and percentiles, no invented SLOs.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
k6 or concurrent integration evidence. Do not claim success without execution evidence.
