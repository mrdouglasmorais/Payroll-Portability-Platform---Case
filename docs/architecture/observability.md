# Observability

## Signals

| Signal | Where | Notes |
|---|---|---|
| Health / readiness | `/actuator/health` | Compose + CI smoke |
| Prometheus scrape | `/actuator/prometheus` | Low-cardinality operational metrics |
| JVM/HTTP metrics | `/actuator/metrics` | Spring Boot defaults |
| Correlation | `X-Correlation-Id` + MDC `correlationId` | Filter echoes header on every response |
| Audit trail | `operation_events` | Append-only financial/process history — authoritative with DB state |

## Portability metrics (API)

- `portability_operations_total{outcome=created|confirmed|rejected}` — incremented **after successful commit** (`TransactionSynchronization.afterCommit`)
- `portability_jobs_claimed_total`
- `portability_jobs_stale_fence_total`
- `portability_jobs_exhausted_total`
- `portability_central_failures_total{class=transient|permanent}`
- `portability_jobs_backlog{state=open|exhausted}`

Never label metrics with beneficiary IDs, operation IDs or account numbers.

### Authority boundary

Operational counters are **not** an authoritative accounting source. Financial truth remains PostgreSQL state (`portability_operations`, reservations, installment snapshots) plus the append-only `operation_events` trail. Do not reconcile money or compliance from Prometheus alone.

## Lab SLIs (not production SLOs)

- Local API availability (`/actuator/health`)
- Submit/poll worker progress (job backlog gauges)
- Integration failure class (transient vs permanent counters)
- Operational outcome rates (confirmed vs rejected counters — non-authoritative)

Tracing backends (OpenTelemetry collectors, Grafana stacks) remain optional and out of the default Compose footprint.
