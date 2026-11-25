# Payroll Portability Platform

Simulated payroll discount portability platform with Java/Spring Boot, React/TypeScript, PostgreSQL and an independent Central simulator.

See [requirements](docs/requirements-traceability.md), [architecture](docs/architecture/overview.md), [contracts](contracts/openapi/) and [AI harness](ai/README.md).

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
```

## Trade-offs

Database-backed jobs avoid broker overhead; polling matches the Central contract; independent simulator exposes partial failures. Future work: production authentication, key rotation, retention policy, operational reconciliation and load validation.

## Scope

Synthetic data only. No real financial transactions. See `AGENTS.md` for engineering rules and `docs/acceptance.md` for release gates.
