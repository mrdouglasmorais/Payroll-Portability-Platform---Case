---
name: devsecops
description: Build reproducible secure delivery. Use when the task touches this engineering concern.
---
# devsecops

## Objective
Build reproducible secure delivery.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Pin dependencies, least privilege, secret scans, CI fail on test errors; isolate simulator controls.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Clean build and scan results. Do not claim success without execution evidence.
