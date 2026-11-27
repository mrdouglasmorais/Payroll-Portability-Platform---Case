# ADR-003: postgresql

Status: Accepted

## Context
Technical challenge requires a maintainable, auditable simulated financial workflow.

## Decision
Separate logical databases for API and Central.

## Alternative
In-memory or shared database.

## Consequences
Durability and realistic isolation; local DB setup.

## Revisit
Reassess if measured constraints or business requirements invalidate this decision.
