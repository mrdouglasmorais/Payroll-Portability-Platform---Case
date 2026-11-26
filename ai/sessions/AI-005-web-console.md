# AI-005 — Web console (M5)

- Task / spec / ADR: SPEC-009, ADR-012, OpenAPI platform
- Tool and model: Cursor agent (Composer)
- Skills loaded: product-design, react-engineering, frontend-architecture, server-state-management, design-system, accessibility, ai-governance
- Constraints: UI polls local API only; mask account; reuse idempotency key on retry; stop polling when terminal; human owns commits

## Changes

- Feature module `PortabilityWizard`: consent → agenda → destination → review → timeline
- Shared API client with TanStack Query mutations/queries
- Account masking on review; integration vs business rejection messaging
- Vitest coverage for mask, terminal polling stop, idempotent resubmit
- Platform `/api/v1/status` milestone → M5

## Validation

```text
$ cd apps/web && pnpm test && pnpm build
Test Files  4 passed (4)
Tests  6 passed (6)
✓ built in 67ms
```

- Validation: PASS
- Human review / decision: pending
- Commit reference: pending (user-executed)
- Remaining risks: no Playwright E2E against live Docker stack; keyboard walkthrough manual; Google Fonts CDN dependency for IBM Plex
