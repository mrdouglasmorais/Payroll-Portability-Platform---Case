# AI-015 — Seed cleanup + Prometheus/Grafana lab stack

## Prompt (human)

Limpar dados de seed e deixar Prometheus + Grafana prontos, usando referências de
https://github.com/mrdouglasmorais/Java-Sentinel-Security-Observability-Lab

## Skills

- observability-engineering
- ai-governance
- database-engineering (Flyway V11 only; no rewrite of V8)
- documentation

## Changes

- Central Flyway `V11__clean_seed_inventory.sql` — remove unused `parcela-seed-3`/`ben-002`
- `infrastructure/prometheus/` — scrape portability-api + central-simulator (5s), lab alerts
- `infrastructure/grafana/` — provisioned Prometheus datasource + Portability ops dashboard
- `docker-compose.yml` — prometheus + grafana (127.0.0.1:9090 / :3000)
- Docs: README, `docs/architecture/observability.md`, `.env.example`

## Rejected suggestions

- Full Sentinel Tempo/Loki/OTel/Keycloak stack (out of scope; user asked Prometheus + Grafana)
- Rewriting historical V8 reopen migration
- Deleting k6 `parcela-load-*` inventory (still required by `tests/performance/k6-m6-portability.js`)

## Validation

```text
docker compose down -v && docker compose up --build -d
# Prometheus targets: portability-api up, central-simulator up, prometheus up
# Grafana /api/health: database=ok version=13.3.1
# Flyway V11 applied; agenda has 30 rows; parcela-seed-3 / ben-002 absent
# Actuator exposes portability_operations_total + portability_jobs_backlog
```

Result: **PASS** (local Compose evidence 2026-11-26).

## Follow-up — IntelliJ + k6 companion

- Prometheus scrapes `host.docker.internal` (IDE or Compose)
- Runbook `docs/runbooks/load-test-intellij-debug.md`
- `./scripts/run-k6-m6.sh debug` + `ide/intellij/runConfigurations/*`

## Risks

- Anonymous Grafana Viewer is lab-only because Grafana binds to 127.0.0.1
- Counters remain non-authoritative for financial reconciliation
