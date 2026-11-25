---
name: ai-governance
description: Record verifiable AI assistance. Use when the task touches this engineering concern.
---
# ai-governance

## Objective
Record verifiable AI assistance.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Capture actual prompts, selected skills, changes, rejected suggestions, commands and human decisions; redact secrets.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Session evidence with commit reference. Do not claim success without execution evidence.
