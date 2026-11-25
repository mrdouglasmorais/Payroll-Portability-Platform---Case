# ADR-007: state-machine

Status: Proposed

## Context
Technical challenge requires a maintainable, auditable simulated financial workflow.

## Decision
Separate business and technical states.

## Alternative
Single status enum.

## Consequences
Avoid false rejection; reconciliation needed.

## Revisit
Reassess if measured constraints or business requirements invalidate this decision.
