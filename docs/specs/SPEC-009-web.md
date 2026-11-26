# SPEC-009-web

Status: Accepted

Source mapping: RF1–RF6. Identifiers assigned locally, not in original challenge.

## Behavior
Operator can register opt-in, query agenda, select installments, enter destination, review, submit and see timeline; UI polls local API only; mask sensitive fields. Console branding uses Cielo tokens/assets; Compose lab serves Vite with HMR on `:5173` (Dockerfile `dev` target + bind-mount). Production-style static image remains Dockerfile `runtime` (nginx).

## Acceptance
End-to-end success/rejection/failure flows; no duplicate on repeated submit; polling stops terminal. Accessibility **smoke** (axe wcag2a/aa + keyboard) across wizard steps on desktop and mobile — not a claim of full WCAG 2.2 AA certification.

## Evidence
- Unit: `PortabilityWizard.test.tsx` (idempotent resubmit, polling stops on terminal), `mask.test.ts`, `pnpm test` / `pnpm build`.
- E2E: Playwright happy path + a11y smoke (`tests/e2e/specs/operator-flow.spec.ts`) against Compose `:5173`.
- Lab seed helper (extra OPEN parcels without reopening PORTED): `scripts/lab-seed-extra-ui-parcels.sh`.
