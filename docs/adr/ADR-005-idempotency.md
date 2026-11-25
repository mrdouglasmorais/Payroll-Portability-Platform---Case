# ADR-005: idempotency

Status: Proposed

## Context
Technical challenge requires a maintainable, auditable simulated financial workflow.

## Decision
Scoped persistent keys and semantic fingerprint in both services.

## Alternative
In-memory dedupe.

## Consequences
Survives restart; retention policy needed.

## Revisit
Reassess if measured constraints or business requirements invalidate this decision.
