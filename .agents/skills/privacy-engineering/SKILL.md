---
name: privacy-engineering
description: Minimize sensitive data. Use when the task touches this engineering concern.
---
# privacy-engineering

## Objective
Minimize sensitive data.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Synthetic fixtures; mask account fields in responses, logs and audit; exclude source PDF from public repo.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Secret and PII scan. Do not claim success without execution evidence.
