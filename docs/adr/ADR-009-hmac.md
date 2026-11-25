# ADR-009: hmac

Status: Proposed

## Context
Technical challenge requires a maintainable, auditable simulated financial workflow.

## Decision
Bidirectional signed HTTP with replay protection.

## Alternative
Unsigned HTTP or mTLS alone.

## Consequences
Integrity; canonicalization and key management needed.

## Revisit
Reassess if measured constraints or business requirements invalidate this decision.
