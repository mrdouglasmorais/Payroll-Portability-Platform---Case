# ADR-001: monorepo

Status: Accepted

## Context
Technical challenge requires a maintainable, auditable simulated financial workflow.

## Decision
One repository for API, simulator, web and docs.

## Alternative
Separate repositories.

## Consequences
Simpler review and atomic contract changes; shared CI coordination.

## Revisit
Reassess if measured constraints or business requirements invalidate this decision.
