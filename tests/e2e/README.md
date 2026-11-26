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

## Lost-response + restart recovery

Simulates platform forgetting `central_protocol` after Central acceptance, verifies the same protocol is restored via idempotent resubmit, restarts `portability-api`, and expects `CONFIRMADA`. Closest automated stand-in for the critical window where Central accepted but local protocol is missing.

```bash
chmod +x tests/e2e/timeout-recovery.sh
./tests/e2e/timeout-recovery.sh
```

## Process-kill recovery (post-persist only)

SIGKILL of `portability-api` **after** local `central_protocol` is durable, then restart → `CONFIRMADA` without duplicate operations. Does **not** kill inside the Central-accept → local-persist window.

```bash
chmod +x tests/e2e/process-kill-recovery.sh
./tests/e2e/process-kill-recovery.sh
```

Optional env: `API_BASE`, `POLL_ATTEMPTS`, `POLL_SLEEP_SEC`.

## Playwright (operator UI + a11y smoke)

Happy path plus axe (`wcag2a`/`wcag2aa`) and keyboard checks across authorization, agenda, selection error, destination, review, status and audit timeline — desktop and mobile viewports. Prefer clean Compose volumes (`down -v`) so `parcela-ui-*` remain OPEN. If parcels were already ported without wiping volumes, seed **additional** OPEN rows (never reopen PORTED):

```bash
./scripts/lab-seed-extra-ui-parcels.sh
```

This is accessibility **smoke**, not WCAG certification.

```bash
cd tests/e2e
pnpm install
pnpm exec playwright install chromium
API_BASE=http://localhost:8080 WEB_BASE=http://localhost:5173 pnpm test
```

## Evidence

Record actual stdout in `ai/sessions/` (do not invent PASS). Differentiate local Compose gates from remote GitHub Actions on a published SHA.
