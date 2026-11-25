# SPEC-003-portability

Status: Proposed

Source mapping: RF3. Identifiers assigned locally, not in original challenge.

## Behavior
Validate beneficiary, consent, distinct eligible installments and destination; reserve installments and atomically create operation/job/audit.

## Acceptance
Concurrent competing operations cannot reserve same installment; invalid selection rejected.

## Evidence
M2 domain create + M4 HTTP/worker: `CreatePortabilityServiceTest`, `PortabilityApiWebTest`, `CentralPortabilityWorkerTest`.
