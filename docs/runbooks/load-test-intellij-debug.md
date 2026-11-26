# Runbook — teste de carga (k6) + debug no IntelliJ

Objetivo: pausar em breakpoints na `portability-api` / `central-simulator` enquanto o k6 gera carga, acompanhando Grafana/Prometheus.

## Visão do fluxo

```text
IntelliJ Debug (:8080 API, :8081 Central)
        ↑ HTTP
   k6 (host ou container)
        ↑ scrape 5s
Prometheus (:9090) → Grafana (:3000)
        ↑ JDBC
Postgres Compose (:5432 / :5433)
```

**Não** rode os containers `portability-api` / `central-simulator` ao mesmo tempo que o Debug no IntelliJ — conflito de porta.

## 1) Infra em Compose (sem as apps Java)

```bash
cp -n .env.example .env
docker compose up -d platform-db central-db otel-lgtm prometheus
docker compose stop portability-api central-simulator web 2>/dev/null || true
```

Confirme:

| Superfície | URL |
|---|---|
| Postgres plataforma | `localhost:5432` |
| Postgres Central | `localhost:5433` |
| Prometheus | http://127.0.0.1:9090 |
| Grafana (LGTM) | http://127.0.0.1:3000 |
| OTLP | `http://127.0.0.1:4318` (export opcional no IDE) |

## 2) Abrir o projeto no IntelliJ

1. **File → Open** a pasta do repositório (ou abra os dois módulos Maven: `apps/portability-api` e `apps/central-simulator`).
2. JDK **25** (Temurin) no Project Structure.
3. Importe Maven nos dois `pom.xml`.

Templates de run configuration (copiar para `.idea/runConfigurations/`):

- `ide/intellij/runConfigurations/central-simulator.debug.xml`
- `ide/intellij/runConfigurations/portability-api.debug.xml`

Ou crie manualmente duas **Spring Boot** configurations:

### Central (Debug primeiro)

- Main: `br.com.payroll.central.CentralSimulatorApplication`
- Module: `central-simulator`
- Env:

```text
SERVER_PORT=8081
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/central
SPRING_DATASOURCE_USERNAME=central
SPRING_DATASOURCE_PASSWORD=central
CENTRAL_HMAC_SECRET=local-test-hmac-secret
CENTRAL_HMAC_KEY_ID=local-test-key
CENTRAL_CONFIRM_DELAY=PT2S
CENTRAL_FAULT_RATE=0
CENTRAL_FAULT_SEED=42
```

### Portability API (Debug)

- Main: `br.com.payroll.portability.PortabilityApiApplication`
- Module: `portability-api`
- Env:

```text
SERVER_PORT=8080
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/portability
SPRING_DATASOURCE_USERNAME=portability
SPRING_DATASOURCE_PASSWORD=portability
CENTRAL_BASE_URL=http://localhost:8081
CENTRAL_HMAC_SECRET=local-test-hmac-secret
CENTRAL_HMAC_KEY_ID=local-test-key
```

Inicie **Central → Debug**, depois **API → Debug**. Health:

- http://localhost:8080/actuator/health  
- http://localhost:8081/actuator/health  

No Prometheus → *Status → Targets*, os jobs devem ficar **UP** via `host.docker.internal`.

## 3) Breakpoints sob carga

| Dica | Motivo |
|---|---|
| Suspend = **Thread** (não All) | Evita congelar o worker inteiro e o k6 estourar timeout |
| Preferir 1 VU no k6 (`K6_VUS=1`) | Um request por vez facilita o step-through |
| Condicionar breakpoint (`idempotencyKey != null`) | Reduz ruído de health/actuator |
| Pontos úteis | `CreatePortabilityService`, worker SUBMIT/POLL, `HmacFilter`, `CentralProtocolService` |

Com breakpoint ativo, aumente mentalmente o timeout: o k6 pode falhar `p(95)` — isso é esperado em sessão de debug, não é SLO.

## 4) Rodar o k6 apontando para o host

Debug (1 VU, ~60s):

```bash
./scripts/run-k6-m6.sh debug
```

Smoke padrão (3 VUs, 45s):

```bash
./scripts/run-k6-m6.sh
```

Equivalente manual (Docker Desktop Mac):

```bash
docker run --rm -i \
  -e API_BASE=http://host.docker.internal:8080 \
  -e K6_VUS=1 \
  -e K6_DURATION=60s \
  grafana/k6:0.54.0 run -e K6_VUS -e K6_DURATION - < tests/performance/k6-m6-portability.js
```

Bootstrap health-only: `tests/performance/k6-m1-bootstrap.js` (ver `tests/performance/README.md`).

## 5) Acompanhar no Grafana

1. Abra http://127.0.0.1:3000 → dashboard **Portability — Operations & Jobs**
2. Durante o k6 observe: RPS, outcomes, backlog de jobs, falhas Central, P95 HTTP
3. Prometheus UI: http://127.0.0.1:9090/graph — ex. `rate(portability_operations_total[1m])`

## 6) Lab limpo entre runs

Parcelas `PORTED`/`RESERVED` sob `ben-001` esgotam sob contenção. Para reset descartável:

```bash
docker compose down -v
docker compose up -d platform-db central-db prometheus grafana
# suba de novo Central + API no IntelliJ Debug
```

Não reabra parcelas PORTED “na mão” em volume persistente (política V9).

## Troubleshooting — “request não chega no IntelliJ”

| Sintoma | Causa típica | Checagem |
|---|---|---|
| Breakpoint na **Central** nunca dispara | Central não está em Debug | `lsof -nP -iTCP:8081 -sTCP:LISTEN` e http://localhost:8081/actuator/health |
| API responde `503` / “Falha de transporte com a Central” | Só a API está up | Suba **central-simulator [Debug]** antes do k6 |
| k6 falha de conexão | API down ou porta errada | http://localhost:8080/actuator/health |
| Prometheus target down | App não escuta ainda | Status → Targets; scrape usa `host.docker.internal` |

Ordem obrigatória: **Central Debug → API Debug → k6**.

Probe manual (deve retornar `201` com os dois apps up):

```bash
curl -sS -X POST http://127.0.0.1:8080/api/v1/autorizacoes \
  -H 'Content-Type: application/json' \
  -H 'X-Correlation-Id: ide-probe' \
  -d '{"beneficiarioId":"ben-ui","convenioId":"conv-1"}'
```

## Checklist rápido

- [ ] Só DBs + Prometheus + Grafana no Compose  
- [ ] Central Debug `:8081`, API Debug `:8080`  
- [ ] `curl` authorize → **201** (não 503)  
- [ ] Targets Prometheus UP  
- [ ] Breakpoint com Suspend=Thread  
- [ ] `./scripts/run-k6-m6.sh debug`  
- [ ] Grafana atualizando  
