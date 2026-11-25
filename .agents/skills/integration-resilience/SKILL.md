---
name: integration-resilience
description: Handle HTTP failures. Use when the task touches this engineering concern.
---
# integration-resilience

## Objective
Handle HTTP failures.

## Procedure
1. Read the applicable spec, ADR and existing code.
2. Identify affected invariants and failure modes.
3. Apply these rules: Bound timeouts; exponential capped backoff with jitter; honor Retry-After; classify 429/5xx/timeout; do not retry permanent invalid requests blindly.
4. Implement the smallest coherent change and positive/negative tests.
5. Record actual validation and unresolved limitations.

## Acceptance gate
Deterministic failure tests. Do not claim success without execution evidence.
