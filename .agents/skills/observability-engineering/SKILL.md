---
name: observability-engineering
description: Instrument operations. Use when the task touches this engineering concern.
---
# observability-engineering

## Objective
Instrument operations.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Correlation IDs, low-cardinality metrics, traces and redacted logs; monitor job lag/retries.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Telemetry smoke test. Do not claim success without execution evidence.
