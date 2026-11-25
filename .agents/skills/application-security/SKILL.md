---
name: application-security
description: Review trust boundaries. Use when the task touches this engineering concern.
---
# application-security

## Objective
Review trust boundaries.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Validate authorization per beneficiary, input and output; restrict CORS; no secrets or real PII.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Threat model and negative authorization tests. Do not claim success without execution evidence.
