# Observability

## Signals

| Signal | Where | Notes |
|---|---|---|
| Health / readiness | `/actuator/health` | Compose + CI smoke |
| Prometheus scrape | `/actuator/prometheus` | Low-cardinality business/job metrics |
| JVM/HTTP metrics | `/actuator/metrics` | Spring Boot defaults |
| Correlation | `X-Correlation-Id` + MDC `correlationId` | Filter echoes header on every response |
| Audit trail | `operation_events` | Append-only financial/process history |

## Portability metrics (API)

- `portability_operations_total{outcome=created|confirmed|rejected}`
- `portability_jobs_claimed_total`
- `portability_jobs_stale_fence_total`
- `portability_jobs_exhausted_total`
- `portability_central_failures_total{class=transient|permanent}`
- `portability_jobs_backlog{state=open|exhausted}`

Never label metrics with beneficiary IDs, operation IDs or account numbers.

## Lab SLIs (not production SLOs)

- Local API availability (`/actuator/health`)
- Submit/poll worker progress (job backlog gauges)
- Integration failure class (transient vs permanent counters)
- Financial outcomes (confirmed vs rejected counters)

Tracing backends (OpenTelemetry collectors, Grafana stacks) remain optional and out of the default Compose footprint.
