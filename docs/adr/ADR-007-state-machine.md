# ADR-007: state-machine

Status: Accepted

## Context
Operators must never confuse integration failure with business rejection (RF06).

## Decision
Separate **business** status (`PENDENTE` / `CONFIRMADA` / `REJEITADA`) from **technical** job status (`PENDING` / `RUNNING` / `FAILED` / `SUCCEEDED` / `EXHAUSTED`).

API projection: when the job is `EXHAUSTED`, `GET` status returns `processing.status = REQUIRES_REVIEW` while business status stays `PENDENTE` until a verified Central terminal outcome. See [contract-divergences.md](../architecture/contract-divergences.md).

## Alternative
Single status enum mixing business and transport failures.

## Consequences
Avoid false rejection; reconciliation needed for `REQUIRES_REVIEW` ([runbook](../runbooks/reconciliation.md)).

## Revisit
Reassess if product requires an explicit persisted `REQUIRES_REVIEW` column (today it is derived).
