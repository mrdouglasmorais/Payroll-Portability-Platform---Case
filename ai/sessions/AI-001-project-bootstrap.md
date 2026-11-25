# AI-001 — Project Bootstrap (M1)

- Task / spec / ADR: M1 executable bootstrap (Docker Compose, Spring Boot apps, React web, CI)
- Tool and model: Cursor agent (Composer)
- Skills loaded: spring-boot-engineering, java-engineering, react-engineering, database-engineering, devsecops, documentation, ai-governance
- Actual prompt (redacted): Implement M0 import + M1 Project Bootstrap plan; user executes commits
- Constraints: No invented evidence; Java 25 + Spring Boot 4; pnpm; no DB transactions across HTTP; synthetic secrets only

## Changes / files

- Imported foundation from `~/Downloads/payroll-portability-foundation` (75 files)
- `docker-compose.yml` — platform-db, central-db, portability-api, central-simulator, web
- `apps/portability-api` — Spring Boot 4.2.0, Flyway V1, Actuator, Maven Wrapper
- `apps/central-simulator` — Spring Boot 4.2.0, Flyway V1, Actuator, Maven Wrapper
- `apps/web` — React 19.3 + Vite 8 + pnpm + Vitest + nginx image
- `.github/workflows/ci.yml` — Maven verify, pnpm test/build, CodeQL, Gitleaks, Trivy
- Toolchain pins documented in root `README.md`

## Commands and actual outputs

```text
$ pnpm test && pnpm build   # apps/web
Test Files  1 passed (1)
Tests  1 passed (1)
✓ built in 239ms

$ docker compose up --build -d
# all services created; healthchecks passed

$ docker compose ps
platform-db         Up (healthy)   5432
central-db          Up (healthy)   5433
central-simulator   Up (healthy)   8081
portability-api     Up (healthy)   8080
web                 Up (healthy)   5173

$ curl -sS http://localhost:8080/actuator/health
{"groups":["liveness","readiness"],"status":"UP"}

$ curl -sS http://localhost:8080/api/v1/status
{"status":"UP","milestone":"M1","service":"portability-api"}

$ curl -sS http://localhost:8081/actuator/health
{"groups":["liveness","readiness"],"status":"UP"}

$ curl -sS http://localhost:8081/status
{"status":"UP","milestone":"M1","service":"central-simulator"}

$ curl -sS -o /dev/null -w "web_http=%{http_code}\n" http://localhost:5173/
web_http=200

$ curl -sS http://localhost:5173/api/v1/status
{"status":"UP","milestone":"M1","service":"portability-api"}
```

- Validation: PASS (compose stack healthy; web unit test + build PASS; k6 M1 load PASS)
- k6 evidence (`tests/performance/k6-m1-bootstrap.js` via `grafana/k6:0.54.0`):
  - 10_076 HTTP reqs, `http_req_failed=0%`, checks `100%`
  - p95: api_health 12.17ms, api_status 6.19ms, central_health 10.54ms, web 3.61ms
- Backend `./mvnw verify` on host: NOT_RUN (host Java is 21; images use Temurin 25)
- Human review / decision: pending
- Commit reference: pending (user-executed commits)
- Remaining risks: OpenAPI contracts still proposed; domain logic not implemented; ADRs remain Proposed until technical review; Gitleaks Action may require org settings on private repos
