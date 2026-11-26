# Session ID / date
- AI-016 — 2026-11-26
- Task / spec / ADR: Structured ECS logs + OpenTelemetry (ADR-014); Notion + DBML author/links; README local start
- Tool and model (if known): Cursor Auto / Composer
- Skills loaded: observability-engineering, ai-governance, documentation, message-integrity (HMAC untouched), async-processing (worker MDC)
- Actual prompt (redacted): Implement observability (SLF4J fluent + ECS JSON stdout, no OTLP logs; OTel starter for traces/metrics; worker Observation/MDC; LogEvents/LogFields/SensitiveData; Compose `otel-lgtm`; tests; ADR/docs/README; Notion + DBML author/links; dual-stack verify)
- Constraints: no HMAC/migration/financial rule changes; CI without collector; IDs not metric tags; audit stays `operation_events`

## Changes / files
- `apps/*/pom.xml` — `spring-boot-starter-opentelemetry`
- `apps/*/application.yml` (+ `application-local.yml`) — ECS console; OTLP export **off** by default
- `shared/observability/` — `LogEvents`, `LogFields`, `SensitiveData` (+ tests)
- Worker/create/finalize/exception handler — event logs; worker Observation + MDC clear
- `AppConfig` / `HttpCentralGateway` — instrumented `RestClient.Builder` (traceparent)
- `docker-compose.yml` — `otel-lgtm` (+ Pyroscope lab args / volume / healthcheck); OTEL env on apps
- Docs: ADR-014, `docs/architecture/observability.md`, README, DBML Refs + author
- Notion: checklist RNF, decisão ADR-014, página “Observabilidade local”, author no parent
- Tests: worker MDC, SensitiveData, StructuredConfirmLogTest
- Session: this file

## Suggestions rejected and why
- OTLP log exporter/appender — couples CI/runtime to collector; stdout ECS is greppable
- High-cardinality metric tags (`operationId`) — series explosion; IDs stay MDC/span attrs
- Keeping orphan Grafana service alongside LGTM on `:3000` — port conflict; LGTM owns Grafana
- Declaring Grafana one-trace 503→success without collector up — wait for LGTM ready first

## Commands and actual outputs

```text
# portability-api
./mvnw -B clean verify
Tests run: 47, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

# central-simulator (after rm -rf target — Finder "* 2" duplicates broke Flyway/classpath)
./mvnw -B clean verify
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

# Compose
docker compose up -d otel-lgtm   # after PYROSCOPE_EXTRA_ARGS + volume
curl http://127.0.0.1:3000/api/health → {"database":"ok","version":"13.3.1",...}

./tests/e2e/dual-stack.sh
DUAL_STACK_E2E_PASS
# Sample log (masked account + traceId):
# "event":"portability.confirmed","receivingAccountMasked":"****99-0","traceId":"..."

# Tempo (inside otel-lgtm): trace batches include service.name=portability-api AND central-simulator
# (parentSpanId set on Central HTTP span — traceparent propagation OK)
```

## Validation
- Unit/integration: **PASS** (47 + 16)
- dual-stack: **PASS**
- Grafana LGTM up: **PASS** (`/api/health`)
- Cross-service trace in Tempo: **PASS** (API → Central same `traceId`)
- Cross-service Tempo span (`portability.job` with `central-simulator` children): **PASS**
- Interactive 503-retry demo end-to-end in one Grafana click-path: **PARTIAL** (instrumentation + LGTM ready; seed `parcela-503-1` exists — create via agenda sync if needed)

## Human review / decision
pending (security/ops: OTLP defaults off; no audit change)

## Commit reference
(none — commit only if requested)

## Remaining risks
- `otel-lgtm` first boot can flap if Pyroscope readiness delay races; mitigated with `PYROSCOPE_EXTRA_ARGS` + volume + healthcheck
- Finder “Name 2.ext” copies under `target/` break Flyway — use `mvn clean` / avoid duplicating resources in Finder
- Optional host Prometheus `:9090` remains beside LGTM’s internal Prometheus (scrape UI still useful for actuator)
