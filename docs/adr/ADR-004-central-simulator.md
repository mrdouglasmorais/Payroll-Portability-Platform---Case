# ADR-004: central-simulator

Status: Accepted

## Context
Technical challenge requires a maintainable, auditable simulated financial workflow.

## Decision
Independent HTTP service with deterministic faults.

## Alternative
Inline mock only.

## Consequences
Tests real failure windows; extra process.

## Revisit
Reassess if measured constraints or business requirements invalidate this decision.
