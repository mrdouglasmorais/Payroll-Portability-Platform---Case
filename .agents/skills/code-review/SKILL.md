---
name: code-review
description: Review independently. Use when the task touches this engineering concern.
---
# code-review

## Objective
Review independently.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Inspect diff against spec; prioritize financial, security and concurrency bugs; state evidence gaps.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Review findings with severity and disposition. Do not claim success without execution evidence.
