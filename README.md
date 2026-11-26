# Payroll Portability Platform

Simulated payroll discount portability platform (Java/Spring Boot + React/TypeScript + PostgreSQL) with an independent **Central de Convênios** simulator. Synthetic data only — no real financial traffic.

Architecture source of truth for decisions: [Miro board](https://miro.com/app/board/uXjVEJ1cEDw=/?share_link_id=843643210573) (indexed in [docs/architecture/system-design.md](docs/architecture/system-design.md)). Requirements: [traceability](docs/requirements-traceability.md), [compliance matrix](docs/compliance-matrix.md), [acceptance](docs/acceptance.md), [release readiness report](docs/release-readiness-report.md), [OpenAPI](contracts/openapi/), [AI harness](ai/README.md), [AGENTS.md](AGENTS.md).

## Overview

Operator console captures beneficiary opt-in, loads the authorized discount agenda, registers portability (one or many installments) with a destination receiving account, tracks async Central confirmation, and shows an append-only audit timeline. Receiving account is applied only after verified `CONFIRMADA`.

## Features

- RF01 Authorization (opt-in) with unique id via Central
- RF02 Scoped future agenda (amount, discount date, agreement, status)
- RF03 Portability create with eligibility, reservation, idempotency
- RF04 Async 202 + protocol + SUBMIT/POLL worker (retry/backoff/lease/fencing)
- RF05 Append-only operation events
- RF06 Rejection vs technical failure (`REQUIRES_REVIEW`, typed `last_error_code`)
- HMAC integrity both directions; lab fault injection on Central

## Architecture (short)

Modular monolith (`portability-api`) + independent Central simulator + React operator UI. Dual PostgreSQL. Persistent leased jobs (no broker). Local transactions; eventual consistency via poll/reconcile. See [docs/architecture/overview.md](docs/architecture/overview.md) and ADRs under `docs/adr/`.

## Stack

| Layer | Choice |
| --- | --- |
| Backend | Java 25, Spring Boot 4.2, JDBC + Flyway |
| Frontend | React 19, TypeScript, Vite, TanStack Query, pnpm |
| Data | PostgreSQL 17 (platform + central) |
| Contracts | OpenAPI 3.1 + Redocly |
| Integrity | HMAC-SHA256 canonical messages |
| Tests | JUnit, Testcontainers, ArchUnit, Vitest, Playwright (+ axe smoke), k6 (lab) |
| Delivery | Docker Compose, GitHub Actions (CodeQL, Gitleaks, Trivy) |

## Prerequisites

- Docker + Docker Compose
- Optional local: Temurin 25, Node 24, pnpm 10.18.2, Maven Wrapper (bundled)

## Quick start

```bash
git clone https://github.com/mrdouglasmorais/Payroll-Portability-Platform---Case.git
cd Payroll-Portability-Platform---Case
cp .env.example .env
docker compose up --build -d
```

Wait until healthy, then open the console.

| Surface | URL |
| --- | --- |
| Operator console (pt-BR) | http://localhost:5173 |
| Portability API health | http://localhost:8080/actuator/health |
| Portability API status | http://localhost:8080/api/v1/status |
| Portability metrics (Prometheus) | http://localhost:8080/actuator/prometheus |
| Central simulator health | http://localhost:8081/actuator/health |
| Central metrics (Prometheus) | http://localhost:8081/actuator/prometheus |

Stop: `docker compose down`. Reset disposable lab volumes: `docker compose down -v`.

## Demo data (synthetic)

| Beneficiary | Purpose |
| --- | --- |
| `ben-ui` / `conv-1` | Operator UI / Playwright (`parcela-ui-*`) |
| `ben-001` | Dual-stack API E2E |
| `ben-recover` | Lost-response / restart harness |
| `*-reject`, `*-503`, `*-timeout` suffixes | Central fault modes |

UI defaults to `ben-ui` / `conv-1`. After a confirmed demo, **do not partially reopen PORTED parcels**. For disposable labs only, recreate volumes:

```bash
docker compose down -v
docker compose up --build -d
```

`scripts/lab-reset-demo-parcels.sh` refuses financial reopen and points to the volume recreate path. That path is **not** safe for persistent financial environments.

### Flyway V8 / V9 (Central)

- `V8__reopen_ui_demo_parcels.sql` historically reopened demo parcels and cleared active reservations on migrate. It remains in Flyway history and **still executes on clean installs** that have not applied it yet.
- `V9__lab_reset_policy.sql` is a **no-op marker** documenting the hazard. It does **not** undo V8 effects on databases that already ran V8.
- Prefer exclusive synthetic beneficiaries per run over mutating shared financial history.

## How to demonstrate

### Scenario A — Confirmed portability

1. Open http://localhost:5173
2. Authorize `ben-ui` / `conv-1`
3. Select an OPEN installment → destination account → review → submit
4. Timeline polls until **Confirmada**; account is applied only then

### Scenario B — Rejected portability

1. Authorize a beneficiary whose id ends with `-reject` (seeded) **or** call Central with fault header in API tests
2. Complete create; timeline ends **Rejeitada**
3. Prior effective receiving account unchanged; reservation released

### Scenario C — Technical failure (not financial rejection)

1. Use `*-503` / timeout seeds or Central unavailable
2. Operation stays non-terminal / `REQUIRES_REVIEW` as applicable
3. Worker retries with backoff; no false `REJEITADA`

### Scenario D — Idempotency

1. Submit once; retry same payload + same `Idempotency-Key` → same operation id
2. Same key + different payload → HTTP 409
3. UI reuses the key on 503 retry; “Nova operação” rotates the key

## How to run tests

```bash
# From repository root
cd apps/portability-api && ./mvnw -B verify && cd ../..
cd apps/central-simulator && ./mvnw -B verify && cd ../..
cd apps/web && pnpm install && pnpm test && pnpm build && cd ../..

npx --yes @redocly/cli@1.34.5 lint --config redocly.yaml \
  contracts/openapi/platform.yaml contracts/openapi/central.yaml

# Compose must be healthy (prefer clean volumes for Playwright parcel budget)
./tests/e2e/dual-stack.sh
./tests/e2e/timeout-recovery.sh
./tests/e2e/process-kill-recovery.sh

cd tests/e2e && pnpm install && pnpm exec playwright install chromium && pnpm test && cd ../..

# Lab-only load smoke (not a production SLO)
docker run --rm -i --network host -e API_BASE=http://localhost:8080 \
  grafana/k6:0.54.0 run - < tests/performance/k6-m6-portability.js
```

### Recovery harnesses (coverage boundary)

| Script | Covers | Does not cover |
| --- | --- | --- |
| `timeout-recovery.sh` | Lost local protocol after Central accept; same key restores protocol; restart → terminal | Exact process death mid-HTTP |
| `process-kill-recovery.sh` | SIGKILL **after** local `central_protocol` persist → recover without duplicate ops | Critical window: Central accepted, local protocol **not** yet durable |

Mitigation for the pre-persist window: reuse the same `Idempotency-Key` (Central returns the original protocol) + [reconciliation runbook](docs/runbooks/reconciliation.md). Deterministic kill in that window would require production test hooks — out of scope.

## How the Central works

Independent Spring Boot service with its own database. Implements the suggested contract (`/autorizacoes`, agenda, `202` + protocolo, status with optional `204` + `Retry-After`). Signs/verifies HMAC, enforces idempotency (raw-body fingerprint), reserves installments, delays confirmation, and supports fault modes (confirm / reject / timeout / unavailable). Platform never holds DB transactions open across Central HTTP calls.

## External libraries (why)

| Library | Why |
| --- | --- |
| Spring Boot | HTTP, JDBC, scheduling, Actuator health for Compose/CI |
| Flyway | Versioned schema + financial constraints |
| PostgreSQL | Relational invariants, partial unique indexes, `FOR UPDATE` |
| Testcontainers | Real Postgres for idempotency/concurrency ITs |
| ArchUnit | Domain must not depend on Spring Web |
| React + TypeScript | Typed operator UI |
| TanStack Query | Server state, polling that stops on terminal status |
| Vite + pnpm | Fast reproducible frontend toolchain |
| Playwright + `@axe-core/playwright` | Operator E2E + accessibility smoke (not WCAG certification) |
| Redocly | OpenAPI lint gate |
| k6 | Optional lab smoke under contention |

## Architectural decisions

See `docs/adr/` (modular monolith, dual DB, persistent jobs, HMAC, reservations, polling, AI harness). Trade-off detail: [idempotency-and-integrity](docs/architecture/idempotency-and-integrity.md), [worker-lease](docs/architecture/worker-lease.md), [reconciliation runbook](docs/runbooks/reconciliation.md), [observability](docs/architecture/observability.md).

## Trade-offs

- DB-backed jobs avoid broker ops cost; polling matches Central contract; fencing + leases handle crashed workers.
- Central fingerprint is raw-body; platform fingerprint is semantic field hash — documented intentionally.
- No operator authentication in lab; HMAC protects service-to-service messages, not end-user identity.
- Prometheus financial counters emit after commit and are **operational only** — not authoritative accounting (DB + audit trail are).
- Future: production auth, key rotation, retention, richer reconciliation CLI, load capacity studies (not claimed here).

## AI harness

Expected by the challenge. Skills live in `.agents/skills/`; sessions in `ai/sessions/` (`AI-000` … `AI-014`). Human review of financial/release findings performed (candidate-owned). Humans own commits. Do not treat AI session narrative as proof without command evidence.

## Evidence (published tip)

| Kind | Value |
| --- | --- |
| SHA | `d0dcbbc4f54b315e1f59008f87b81d4781c2c5d6` |
| Remote CI | [SUCCESS 8/8](https://github.com/mrdouglasmorais/Payroll-Portability-Platform---Case/actions/runs/45740557504) |
| Session | `ai/sessions/AI-014-final-release-hardening.md` |

## Known limitations

- Lab-only: no real banks, no operator SSO, synthetic beneficiaries.
- `V8` demo reopen remains in Flyway history on clean Central installs; `V9` does not undo it; disposable volume wipe only.
- `process-kill-recovery.sh` covers post-persist SIGKILL only; pre-persist critical window is a documented residual.
- Accessibility checks are automated smoke (axe wcag2a/aa + keyboard) across wizard steps — **not** full WCAG 2.2 AA certification.
- Operator auth, soak tests, retention/PII policies, and production-grade secret management remain future work.

## Milestones

- **M0–M9**: foundation → financial correctness (see git history)
- **AI-010**: residual races + lab reset policy
- **AI-011**: operator UI pt-BR
- **AI-012**: release readiness / lock order / CI & docs
- **M10 / AI-013**: observability metrics, request hardening, manual validation DONE
- **AI-014**: final release hardening (docs honesty, afterCommit metrics, a11y smoke expansion)
