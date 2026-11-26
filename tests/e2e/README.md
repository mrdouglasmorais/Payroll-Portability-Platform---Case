# tests/e2e

Executable dual-stack checks against a running Compose stack (API + Central + DBs + web).

## Prerequisites

```bash
cp .env.example .env
docker compose up --build -d
# wait until healthchecks are healthy
```

## Dual-stack API flow

Covers authorize → agenda → create (idempotent replay + 409 mismatch) → poll to `CONFIRMADA`, then reject path to `REJEITADA`.

```bash
chmod +x tests/e2e/dual-stack.sh
./tests/e2e/dual-stack.sh
```

Optional env: `API_BASE`, `POLL_ATTEMPTS`, `POLL_SLEEP_SEC`.

## Playwright (operator UI)

```bash
cd tests/e2e
pnpm install
pnpm exec playwright install chromium
API_BASE=http://localhost:8080 WEB_BASE=http://localhost:5173 pnpm test
```

## Evidence

Record actual stdout in `ai/sessions/` (do not invent PASS). CI still runs unit/integration per app; this suite is Compose-backed and intended for local/release gates.
