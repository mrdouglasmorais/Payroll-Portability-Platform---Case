---
name: end-to-end-testing
description: Validate complete workflow. Use when the task touches this engineering concern.
---
# end-to-end-testing

## Objective
Validate complete workflow.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Playwright exercises consent, selection, confirmation, rejection and audit; avoid fixed sleeps.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
E2E reports. Do not claim success without execution evidence.
