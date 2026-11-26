# ADR-014: observability

Status: Accepted (revised)

## Context
Operators need correlation across HTTP and async workers, low-cardinality metrics, and a way to diagnose multi-hop latency (plataforma → Central) without a heavyweight production mesh. Financial audit remains append-only `operation_events` — logs are operational only.

## Decision
1. **Logs**: JSON on stdout via Spring Boot structured logging (`logging.structured.format.console: ecs`). Fluent SLF4J (`addKeyValue`) with stable `event` names. Profile `local` keeps text console for IDE comfort. **No** OTLP log exporter/appender.
2. **Traces + OTLP metrics**: `spring-boot-starter-opentelemetry` exporting over OTLP HTTP when `OTEL_EXPORTER_OTLP_ENDPOINT` is set. Without endpoint, apps start normally (CI/tests).
3. **Prometheus scrape**: keep `/actuator/prometheus` and Micrometer counters (financial outcomes still `afterCommit`).
4. **Correlation**: business `X-Correlation-Id` (HMAC canonical) + W3C `traceparent` via instrumented `RestClient`. Worker opens an `Observation` (`portability.job`) and puts `operationId`/`jobId`/… in MDC (not metric tags).
5. **Lab stack**: Compose `otel-lgtm` (Grafana :3000 + OTLP :4317/:4318) plus optional Prometheus scrape :9090.

## Alternatives considered
| Option | Why not |
|---|---|
| OTLP log exporter instead of stdout JSON | Couples CI/runtime to collector; stdout ECS is greppable and shippable later |
| OpenTelemetry Java Agent only | Harder to reason about in Boot tests; starter is first-class in Boot 4.2 |
| Full Sentinel Tempo/Loki/Keycloak Compose from day one | Too heavy for CI; LGTM covers lab traces/logs/metrics in one image |
| High-cardinality metric labels (`operationId`) | Explodes series; IDs stay in MDC/spans only |

## Consequences
- Lab can follow a 503→retry→success as one trace in Grafana (Tempo) and filter logs by `traceId`.
- Metrics remain non-authoritative for money; ledger = Postgres + `operation_events`.
- Sensitive account/agency values must be masked (`SensitiveData`) in logs.

## Evidence
`docs/architecture/observability.md`, Compose `otel-lgtm`, AI-016 session.
