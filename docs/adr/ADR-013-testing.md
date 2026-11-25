# ADR-013: testing

Status: Proposed

## Context
Technical challenge requires a maintainable, auditable simulated financial workflow.

## Decision
Behavior tests with real DB and HTTP stubs.

## Alternative
Unit-only.

## Consequences
Confidence in failure modes; CI time.

## Revisit
Reassess if measured constraints or business requirements invalidate this decision.
