# ADR-011: reservations

Status: Accepted

## Context
Technical challenge requires a maintainable, auditable simulated financial workflow.

## Decision
Unique active installment reservation in both systems.

## Alternative
Application-only check.

## Consequences
Concurrency protection; ambiguous cases hold reservation.

## Revisit
Reassess if measured constraints or business requirements invalidate this decision.
