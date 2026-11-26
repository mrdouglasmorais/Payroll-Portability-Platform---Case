# apps/web

Console operacional (pt-BR) em React 19 + TypeScript + Vite para a plataforma de portabilidade de folha. Branding Cielo (tokens/assets locais sob `public/brand`).

## Compose (lab)

`docker compose` builds the Dockerfile **`dev`** target and bind-mounts `./apps/web` so edits hot-reload on http://localhost:5173. The container proxies `/api` and `/actuator` to `portability-api`.

Static nginx image (target `runtime`) remains available for production-style builds; Compose lab uses Vite.

## Local commands (host)

```bash
pnpm install
pnpm dev
pnpm test
pnpm lint
pnpm build
```

Dev server listens on `:5173` (`strictPort`) and proxies `/api` / `/actuator` to `http://localhost:8080` (override with `VITE_PROXY_TARGET`).

Do not run host `pnpm dev` while Compose `web` already owns `:5173`.

Package manager: pnpm.
