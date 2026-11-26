# SPEC-009-web

Status: Proposed

Source mapping: RF1–RF6. Identifiers assigned locally, not in original challenge.

## Behavior
Operator can register opt-in, query agenda, select installments, enter destination, review, submit and see timeline; UI polls local API only; mask sensitive fields.

## Acceptance
Accessible end-to-end success/rejection/failure flows; no duplicate on repeated submit; polling stops terminal.

## Evidence
M5: `PortabilityWizard.test.tsx` (idempotent resubmit, polling stops on terminal), `mask.test.ts`, `pnpm build`.
