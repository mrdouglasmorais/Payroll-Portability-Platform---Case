# tests/performance

Load tests with [k6](https://k6.io/). M1 covers bootstrap health/status only.

## Prerequisites

```bash
docker compose up --build -d
```

## Run M1 load test

```bash
docker run --rm -i --network host \
  -e API_BASE=http://localhost:8080 \
  -e CENTRAL_BASE=http://localhost:8081 \
  -e WEB_BASE=http://localhost:5173 \
  grafana/k6:0.54.0 run - < tests/performance/k6-m1-bootstrap.js
```

On Docker Desktop for Mac, if `--network host` cannot reach published ports, use `host.docker.internal`:

```bash
docker run --rm -i \
  -e API_BASE=http://host.docker.internal:8080 \
  -e CENTRAL_BASE=http://host.docker.internal:8081 \
  -e WEB_BASE=http://host.docker.internal:5173 \
  grafana/k6:0.54.0 run - < tests/performance/k6-m1-bootstrap.js
```

## What it measures

| Target | Path |
| --- | --- |
| Portability API | `/actuator/health`, `/api/v1/status` |
| Central simulator | `/actuator/health` |
| Web | `/` |

Scenarios: 5 VUs smoke (30s), then ramp to 20 VUs. Thresholds are bootstrap guards (`http_req_failed < 1%`, p95 health/status `< 500ms`), not production SLOs.

Domain concurrency (idempotency keys, competing reservations) lands with M4+ scripts.
