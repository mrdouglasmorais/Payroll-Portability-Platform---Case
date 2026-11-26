# AI-020 — Validation, residual cleanup, docs alignment

**Date:** 2026-11-26  
**Author:** Douglas Morais  
**Skills:** ai-governance, documentation, test-engineering, accessibility  

## Prompt (redacted)

Realize os testes; garanta ausência de código residual / gaps técnicos; documentação alinhada aos requisitos. Em seguida conclua e aplique commits sem co-autor.

## Gaps found

| Gap | Action |
| --- | --- |
| `c65e84a` message claimed Vite HMR but tree was empty | Restored Dockerfile `dev` target, Compose bind-mount `:5173`, vite polling/`strictPort`, package scripts |
| Unused brand/Vite boilerplate assets | Removed unused `public/brand/*` duplicates, `src/assets/*`, empty `App.css`; slimmed `Icons.tsx` to used exports |
| Missing `scripts/lab-seed-extra-ui-parcels.sh` | Restored (INSERT OPEN only; never reopen PORTED) |
| Playwright selectors stale vs Cielo UI | Updated to `Nova operação`, `Autorizar e consultar agenda`, step titles |
| Docs still said Nginx `:5173` | `system-design`, SPEC-009, web/infra README, `docs/verification.md` |

## Commands and results

```text
$ cd apps/web && pnpm test
Tests 15 passed (15)

$ cd apps/web && pnpm lint && pnpm exec tsc -b && pnpm build
oxlint warnings only (Toast export / usePortabilityFlow effects) — BUILD OK

$ cd apps/portability-api && ./mvnw -B test
Tests run: 50, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS

$ cd apps/central-simulator && ./mvnw -B test
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS

$ ./scripts/lab-seed-extra-ui-parcels.sh
LAB_SEED_OK

$ export PLAYWRIGHT_BROWSERS_PATH="$HOME/Library/Caches/ms-playwright"
$ cd tests/e2e && API_BASE=http://localhost:8080 WEB_BASE=http://localhost:5173 pnpm exec playwright test
  3 passed (15.0s)
```

Playwright 1.65 required `pnpm exec playwright install --force chromium` into `$HOME/Library/Caches/ms-playwright` (revision 1255). E2E selectors updated for demo consent UI + `parcela-ui-extra-*` selection (Central OPEN vs platform CONSUMED drift).

## Unresolved

- Remote CI not claimed for unpublished tip (`main` ahead of `origin/main`). Acceptance tip remains published green SHA until a new Actions run.
- Oxlint warnings in Toast / usePortabilityFlow left as pre-existing style signals (no behavior change).

