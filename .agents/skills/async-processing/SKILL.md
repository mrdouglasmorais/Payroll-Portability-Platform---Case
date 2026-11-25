---
name: async-processing
description: Build persistent jobs. Use when the task touches this engineering concern.
---
# async-processing

## Objective
Build persistent jobs.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Claim via short transaction and lease/fencing token; retry outside transaction; persist next attempt; recover expired leases.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Restart and stale-worker tests. Do not claim success without execution evidence.
