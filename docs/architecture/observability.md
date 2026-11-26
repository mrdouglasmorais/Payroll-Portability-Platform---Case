# Observability

Author lab notes: Douglas Morais. Stack: ECS JSON logs (stdout) + OpenTelemetry OTLP (traces/metrics) + Micrometer Prometheus scrape. Pattern inspired by [Java-Sentinel-Security-Observability-Lab](https://github.com/mrdouglasmorais/Java-Sentinel-Security-Observability-Lab) and [Notion workspace](https://app.notion.com/p/424e56450c38818bb71fc51de037a438?v=424e56450c388121ab7f000c7541fddc&t=424e56450c3880448a0200a9a19721d3).

## Surfaces

| Surface | URL | Notes |
|---|---|---|
| Grafana (LGTM) | http://127.0.0.1:3000 | Traces (Tempo), logs (Loki), metrics |
| OTLP HTTP | `otel-lgtm:4318` / host `127.0.0.1:4318` | Set `OTEL_EXPORTER_OTLP_ENDPOINT` |
| Prometheus scrape UI | http://127.0.0.1:9090 | Optional; scrapes `/actuator/prometheus` |
| Health | `/actuator/health` | Compose + CI |
| Prometheus endpoint | `/actuator/prometheus` | Always on; CI-safe without LGTM |

## Finding gaps (logs · traces · throughput)

1. **Throughput / error rate** — Grafana Explore → Prometheus/Mimir or http://127.0.0.1:9090:  
   `sum(rate(http_server_requests_seconds_count{application="portability-api",uri!~".*/actuator.*"}[1m]))`  
   `sum(rate(portability_operations_total[1m])) by (outcome)`  
   `portability_jobs_backlog{state="open"}`
2. **Trace of a failed then recovered portability** — Grafana → Explore → Tempo: search by service `portability-api`, span `portability.job` or HTTP to Central. One trace should show SUBMIT retries then POLL success.
3. **Logs for that trace** — copy `traceId` from the span → Explore → Loki/logs: filter `{service_name="portability-api"}` and `trace.id` / `traceId`. Or grep container logs for `"event":"central.call.failed"`.
4. **Business correlation** — header/MDC `correlationId` / field `correlationId` (also in `operation_events`).

## Event catalog (stdout JSON)

| Event | Level | When |
|---|---|---|
| `portability.created` | INFO | Create committed |
| `idempotency.replay` | INFO | Same key+hash |
| `idempotency.conflict` | WARN | Same key, different hash |
| `reservation.conflict` | WARN | Parcel not OPEN / race |
| `central.submit.accepted` | INFO | Central 202 + protocol |
| `central.poll.pending` | INFO | Still PENDENTE |
| `central.call.failed` | WARN | Transient/permanent Central error (`errorCode`, `attempt`) |
| `portability.confirmed` | INFO | Terminal confirm (masked account) |
| `portability.rejected` | INFO | Terminal reject |
| `job.fence.stale` | WARN | Fence lost |
| `job.exhausted` | ERROR | REQUIRES_REVIEW |

Fields: `event`, `operationId`, `jobId`, `correlationId`, `attempt`, `fenceToken`, `jobType`, `protocol`, `errorCode`, `receivingBank`, `receivingAgencyMasked`, `receivingAccountMasked`.  
Never log: HMAC secret/signature, raw bodies, full agency/account, real PII.

## Metrics (API)

- `portability_operations_total{outcome=created|confirmed|rejected}` — **afterCommit**
- `portability_jobs_claimed_total` / `stale_fence` / `exhausted`
- `portability_central_failures_total{class=transient|permanent}`
- `portability_jobs_backlog{state=open|exhausted}`

IDs are **not** metric labels.

### Authority boundary

Operational counters ≠ accounting. Truth = PostgreSQL + `operation_events`.

## Local Compose

```bash
docker compose up --build -d
# Grafana LGTM: http://127.0.0.1:3000  (admin / GRAFANA_ADMIN_PASSWORD)
# Find a trace: Explore → Tempo → search portability-api → open span → Logs for traceId
# Tip: filter Tempo by service.name=portability-api and look for job.type / HTTP to Central.
```

IDE text logs: `SPRING_PROFILES_ACTIVE=local`. Without `OTEL_EXPORTER_OTLP_ENDPOINT`, export is off (tests/CI).

Lab note: `otel-lgtm` may need ~30–90s on first boot (Pyroscope readiness). Compose sets `PYROSCOPE_EXTRA_ARGS` for single-instance lab.

## Grafana boards (LGTM)

Provisionados em Compose (`otel-lgtm` mounts):

1. **Portability — Traceability** (home) — ordem fixa: roteiro → saúde → throughput/erros → outcomes → worker → latência → Tempo → logs → USE.
2. **Portability — Ops** — snapshot compacto.

Métricas no Prometheus do LGTM usam sufixo `_milliseconds` (OTLP). Scrape host `:9090` usa `_seconds` (actuator).

## Seed inventory (Central)

Documentação apenas: [seed-inventory.md](seed-inventory.md). `V11` é no-op — **sem** clean seed executável. Reset: `docker compose down -v`.
