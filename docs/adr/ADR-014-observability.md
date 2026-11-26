# ADR-014: observability

Status: Accepted

## Context
Operators need correlation, job backlog visibility and integration failure class without a heavyweight telemetry platform.

## Decision
Actuator health + Micrometer Prometheus registry + MDC correlation IDs + low-cardinality portability counters/gauges. Full OTel collectors remain optional.

## Alternative
Ship OpenTelemetry + Grafana stack in Compose from day one.

## Consequences
Useful local diagnosis (`/actuator/prometheus`); no high-cardinality labels; tracing deferred.

## Revisit
Add tracing exporters when multi-service latency diagnosis requires it.

## Evidence
See `docs/architecture/observability.md`.
