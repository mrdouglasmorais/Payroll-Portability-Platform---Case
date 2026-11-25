# ADR-006: persistent-jobs

Status: Proposed

## Context
Technical challenge requires a maintainable, auditable simulated financial workflow.

## Decision
Transactional job table with lease and fencing.

## Alternative
Kafka or in-memory scheduler.

## Consequences
Atomic enqueue; polling overhead.

## Revisit
Reassess if measured constraints or business requirements invalidate this decision.
