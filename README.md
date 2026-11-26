# Payroll Portability Platform

Simulated payroll discount portability platform with Java/Spring Boot, React/TypeScript, PostgreSQL and an independent Central simulator.

See [requirements](docs/requirements-traceability.md), [system design (balizador Miro)](docs/architecture/system-design.md), [architecture overview](docs/architecture/overview.md), [contracts](contracts/openapi/) and [AI harness](ai/README.md).

Board Miro (fonte das decisões de arquitetura): https://miro.com/app/board/uXjVEJ1cEDw=/?share_link_id=843643210573


## Milestones

- **M0** Engineering foundation (skills, ADRs, specs, OpenAPI)
- **M1** Executable bootstrap (`docker compose up --build`)
- **M2** Domain & persistence (aggregates, Flyway V2, JDBC, invariant tests)
- **M3** Central simulator (HMAC, protocol, idempotency, fault injection)
- **M4** Portability core (HTTP API, Central HMAC client, persistent worker)
- **M5** Web console (operator flow, TanStack Query, accessible timeline)
- **M6** Quality hardening (dual-stack E2E, Playwright, k6 lab smoke, acceptance mapping)
- **M7** Failure recovery (lost-response protocol restore + worker restart resume)
- **M8** Financial integrity (lease reclaim, fencing, effective account, ops visibility)
- **M9** Financial correctness (transactional fence lock, Central effective account, typed errors, OpenAPI CI)

## Quick start (M1)

```bash
cp .env.example .env
docker compose up --build
```

| Surface | URL |
| --- | --- |
| Web console | http://localhost:5173 |
| Portability API health | http://localhost:8080/actuator/health |
| Portability API status | http://localhost:8080/api/v1/status |
| Central simulator health | http://localhost:8081/actuator/health |

### Local toolchains

| Component | Pin |
| --- | --- |
| Backend | Java 25 + Spring Boot 4.2.0 |
| Build | Maven Wrapper |
| Frontend | React 19 + TypeScript + Vite + pnpm |
| Database | PostgreSQL 17 + Flyway |
| Tests | JUnit, Testcontainers, ArchUnit, Vitest |
| CI | GitHub Actions + CodeQL + Gitleaks + Trivy |

```bash
# API
cd apps/portability-api && ./mvnw verify

# Simulator
cd apps/central-simulator && ./mvnw verify

# Web
cd apps/web && pnpm install && pnpm test && pnpm build

# Dual-stack E2E (Compose must be healthy)
./tests/e2e/dual-stack.sh
./tests/e2e/timeout-recovery.sh

# Playwright UI (optional)
cd tests/e2e && pnpm install && pnpm exec playwright install chromium && pnpm test

# k6 lab smoke (optional; not a production SLO)
docker run --rm -i --network host -e API_BASE=http://localhost:8080 \
  grafana/k6:0.54.0 run - < tests/performance/k6-m6-portability.js
```


## Trade-offs

Database-backed jobs avoid broker overhead; polling matches the Central contract; independent simulator exposes partial failures. Future work: production authentication, key rotation, retention policy, operational reconciliation and load validation.

## Scope

Synthetic data only. No real financial transactions. See `AGENTS.md` for engineering rules and `docs/acceptance.md` for release gates.
