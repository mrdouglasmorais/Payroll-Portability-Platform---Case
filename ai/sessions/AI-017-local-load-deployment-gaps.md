# AI-017 — Local load tests + deployment gaps

**Date:** 2026-11-26  
**Author:** Douglas Morais  
**Goal:** Run M1/M6 locally against Compose; analyze logs/metrics/traces; list deployment gaps.

## Commands (evidence)

```bash
docker compose ps
curl -sf http://127.0.0.1:8080/actuator/health
curl -sf http://127.0.0.1:8081/actuator/health

# M1 bootstrap (PASS)
docker run --rm -i \
  -e API_BASE=http://host.docker.internal:8080 \
  -e CENTRAL_BASE=http://host.docker.internal:8081 \
  -e WEB_BASE=http://host.docker.internal:5173 \
  grafana/k6:0.54.0 run - < tests/performance/k6-m1-bootstrap.js
# → checks 100%, http_req_failed 0%, ~90 req/s, p95 api_health ~13ms (max spike ~1s)

# M6 via ./scripts/run-k6-m6.sh (FAIL tooling — API_BASE cleared)
# → dial tcp 127.0.0.1:8080 connection refused; errors 100%

# M6 after fixing -e VAR=value (smoke 3 VU/45s) — FAIL product until Central auths restored
# → authorize 201 OK; agenda 0% (403 Agenda da Central proibida)

# Lab repair: re-insert Central authorizations matching platform ACCEPTED consents
# Then M6 smoke PASS: checks 100%, errors 0%, create p95 ~16ms
# M6 heavy 10 VU/90s PASS: checks 100%, ~57 req/s, http_req_failed 0%
#   (create mostly 409 after OPEN parcels exhausted; 20× 202 → CONFIRMADA)
```

## Outcomes (DB / metrics)

| Signal | Result |
| --- | --- |
| `portability_operations` | CONFIRMADA 23 ( +20 this run), REJEITADA 2 |
| Central `agenda_installments` ben-001 | PORTED 20, OPEN 2 |
| `processing_jobs` | SUCCEEDED 25, backlog open/exhausted = 0 |
| Structured API events | `reservation.conflict` 1942 WARN; `central.submit.accepted` 20; `portability.confirmed` 20 |
| Central structured `event` | **0** under load |
| Host Prometheus `up` | portability-api, central-simulator, prometheus = 1 |
| LGTM Prometheus | `*_milliseconds` present; rate ~22 rps non-actuator |
| Tempo | `portability.job` + Central `http get /operacoes-portabilidade/{protocolo}` |
| Loki | **empty** (expected ADR-014 — no OTLP logs) |

## Gaps (implantation / ops)

1. **`scripts/run-k6-m6.sh` cleared k6 env** — `k6 run -e API_BASE` without `=value` zeros `__ENV.API_BASE` → container hits `localhost:8080`. **Fixed** in this session (`-e API_BASE=$API_BASE` form).
2. **Dual-DB desync (blocking M6)** — Platform `RequestAuthorizationService` short-circuits on local ACCEPTED consent and does **not** re-POST `/autorizacoes` to Central. Recreating only `central-db` leaves platform consents without Central `authorizations` → agenda **403** while authorize still returns 201. No seed for runtime authorizations; no readiness check for this invariant.
3. **403 agenda not logged as domain `event`** — `ResponseStatusException` handler returns ProblemDetail with no ECS `event`/`trace`-correlated WARN (unlike `reservation.conflict`). Investigation relied on HTTP metrics (`status=403` count 259) + manual curl.
4. **Central simulator emits no structured `event` logs** under load — stdout lacks the ECS event vocabulary used by the platform; Grafana step 7 is API-only in practice.
5. **Loki panel empty by design** — Traceability board documents stdout; Loki query panel has no ingest path. Operators must use `docker compose logs` (or add a log shipper — out of ADR-014 scope).
6. **Dual Prometheus naming** — Actuator scrape (host `:9090`) = `*_seconds`; OTLP→LGTM = `*_milliseconds`. Dashboards target LGTM; host Prometheus queries for the same board expressions return empty.
7. **M6 sustained create throughput** — After ~20 OPEN parcels, heavy run is mostly contention `409` (expected). Load profile does not reseed agenda; end-to-end confirm path is only exercised in the first wave.
8. **WARN noise** — 1942× `reservation.conflict` at WARN under intentional contention may drown rarer signals (`central.call.failed`, `job.exhausted`) in stdout greps.

## Unresolved risks

- No automated compose “warmup” that ensures Central authorizations exist for seeded platform consents / load beneficiaries.
- Host `:9090` vs LGTM Prometheus confusion for evaluators opening the wrong datasource.
- Sampling is 1.0 locally; production sampling not validated under this run.

## Code change this session

- `scripts/run-k6-m6.sh` — pass explicit `VAR=value` to `k6 run -e`.
