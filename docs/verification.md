# Verification log (local)

Honest local gates for unpublished tip. Remote CI claims stay on the last published green SHA only (see [acceptance.md](acceptance.md)).

## Scope covered this pass

| Gate | Command | Result |
| --- | --- | --- |
| Web unit | `cd apps/web && pnpm test` | Record in `ai/sessions/AI-020-validation-docs.md` |
| Web lint/type/build | `pnpm lint && pnpm exec tsc -b && pnpm build` | Same session |
| Portability API unit | `./mvnw -B test` (module) | Same session |
| Central unit | `./mvnw -B test` (module) | Same session |
| Playwright | `cd tests/e2e && pnpm exec playwright test` (Compose healthy; browsers in `$HOME/Library/Caches/ms-playwright`) | **3 passed** (AI-020) |

## Compose web runtime

Lab Compose uses Dockerfile target **`dev`** (Vite `:5173` + bind-mount). Do not document the lab URL as nginx `:80`. Static nginx remains target **`runtime`** for production-style images only.

## Residuals

- Unpublished commits ahead of `origin/main` must not claim remote Actions green until a new run.
- Extra UI parcels for repeated demos: `scripts/lab-seed-extra-ui-parcels.sh` (insert OPEN only).
