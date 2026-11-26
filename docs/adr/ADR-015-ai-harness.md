# ADR-015: ai-harness

Status: Accepted

## Context
Technical challenge requires a maintainable, auditable simulated financial workflow with recorded AI assistance.

## Decision
Versioned skills, specs, prompt and evidence records under `ai/sessions/` (`AI-000` … current). Humans own commits and financial invariant approval. Compliance matrix RNF08 tracks session evidence + human review — not AI narrative alone.

## Alternative
Ad hoc AI usage without session evidence.

## Consequences
Reviewable process; documentation effort; stale compliance docs are treated as RNF08 defects.

## Revisit
Reassess if measured constraints or business requirements invalidate this decision.
