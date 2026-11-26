# SPEC-009-web

Status: Proposed

Source mapping: RF1–RF6. Identifiers assigned locally, not in original challenge.

## Behavior
Operator can register opt-in, query agenda, select installments, enter destination, review, submit and see timeline; UI polls local API only; mask sensitive fields.

## Acceptance
End-to-end success/rejection/failure flows; no duplicate on repeated submit; polling stops terminal. Accessibility **smoke** (axe wcag2a/aa + keyboard) across wizard steps on desktop and mobile — not a claim of full WCAG 2.2 AA certification.

## Evidence
M5: `PortabilityWizard.test.tsx` (idempotent resubmit, polling stops on terminal), `mask.test.ts`, `pnpm build`.
AI-014: Playwright happy path + a11y smoke (`tests/e2e/specs/operator-flow.spec.ts`) — 3 passed on clean Compose volumes.
