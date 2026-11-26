# tests/performance

Load tests with [k6](https://k6.io/). Thresholds are lab smoke gates, not production SLOs.

## Prerequisites

**Full Compose** (apps in Docker):

```bash
docker compose up --build -d
```

**IntelliJ Debug** (apps on the host — preferred for breakpoints):

```bash
docker compose up -d platform-db central-db prometheus grafana
docker compose stop portability-api central-simulator web 2>/dev/null || true
# then Debug Central :8081 + API :8080 in IntelliJ
```

Full walkthrough: [docs/runbooks/load-test-intellij-debug.md](../../docs/runbooks/load-test-intellij-debug.md).

## M6 — portability create under contention

```bash
./scripts/run-k6-m6.sh          # 3 VUs / 45s
./scripts/run-k6-m6.sh debug    # 1 VU / 60s, relaxed thresholds for breakpoints
```

Env overrides: `API_BASE`, `K6_VUS`, `K6_DURATION`, `K6_SKIP_THRESHOLDS=1`.

Manual (Docker Desktop Mac):

```bash
docker run --rm -i \
  -e API_BASE=http://host.docker.internal:8080 \
  grafana/k6:0.54.0 run - < tests/performance/k6-m6-portability.js
```

| Signal | Meaning |
| --- | --- |
| authorize / create / status trends | Lab latency under contention |
| empty agenda mid-run | Expected when OPEN parcels are exhausted |
| Grafana dashboard | http://127.0.0.1:3000 |

## M1 — bootstrap health/status only

```bash
docker run --rm -i \
  -e API_BASE=http://host.docker.internal:8080 \
  -e CENTRAL_BASE=http://host.docker.internal:8081 \
  -e WEB_BASE=http://host.docker.internal:5173 \
  grafana/k6:0.54.0 run - < tests/performance/k6-m1-bootstrap.js
```

| Target | Path |
| --- | --- |
| Portability API | `/actuator/health`, `/api/v1/status` |
| Central simulator | `/actuator/health` |
| Web | `/` |

Scenarios: 5 VUs smoke (30s), then ramp to 20 VUs. Thresholds: `http_req_failed < 1%`, p95 health/status `< 500ms`.
