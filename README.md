# Plataforma de Portabilidade de Folha

**Author:** Douglas Morais ([@mrdouglasmorais](https://github.com/mrdouglasmorais))

Simulação de portabilidade de descontos em folha (Java/Spring Boot + React + PostgreSQL) com Central de Convênios simulada. Dados sintéticos apenas.

Docs (PT): [Notion](https://app.notion.com/p/424e56450c38818bb71fc51de037a438?v=424e56450c388121ab7f000c7541fddc&t=424e56450c3880448a0200a9a19721d3) · [Miro — começar no mapa 00](https://miro.com/app/board/uXjVEJ1cEDw=/?share_link_id=843643210573&moveToWidget=3458764691629361954) · [DBML](docs/architecture/data-model.md) · [AGENTS.md](AGENTS.md)

## Início rápido (local)

Requisito: Docker Desktop (ou Engine + Compose).

```bash
git clone https://github.com/mrdouglasmorais/Payroll-Portability-Platform---Case.git
cd Payroll-Portability-Platform---Case
cp .env.example .env
docker compose up --build -d
```

Espere os healthchecks e abra:

| O quê | URL |
| --- | --- |
| Console | http://localhost:5173 |
| API health | http://localhost:8080/actuator/health |
| Central health | http://localhost:8081/actuator/health |
| Grafana (logs / traces / métricas) | http://127.0.0.1:3000 |
| Prometheus scrape UI | http://127.0.0.1:9090 |

Demo: no console use `ben-ui` / `conv-1` → autorize → escolha parcela → envie → acompanhe até **Confirmada**.

Parar: `docker compose down` · reset total: `docker compose down -v`

Sem Docker (JDK 25 + 2 Postgres + pnpm): [docs/dev/maven.md](docs/dev/maven.md) e seção *Rodando sem Docker* abaixo. Debug + k6: [runbook IntelliJ](docs/runbooks/load-test-intellij-debug.md).

## Observabilidade local

Compose sobe `otel-lgtm` (Grafana + Tempo + Loki + OTLP em `:4318`). Apps exportam traces/métricas via OTLP; logs vão em **JSON ECS no stdout** (um evento por transição — sem spam).

```bash
# Já coberto por: docker compose up --build -d
open http://127.0.0.1:3000   # admin / valor de GRAFANA_ADMIN_PASSWORD
```

Dashboard home no Grafana: **Portability — Traceability** (ordem fixa saúde → throughput → outcomes → worker → latência → Tempo → logs → USE). Compacto: **Portability — Ops**.

Como achar gaps (mesma ordem do board):

1. **Throughput / erros** — painéis RED do dashboard (ou Prometheus `:9090` scrape opcional).
2. **Trace** — seção Tempo / Explore → `portability.job` (API→Central no mesmo `traceId`).
3. **Logs** — `docker compose logs portability-api | rg '"event"|<traceId>'` (ECS stdout; sem OTLP logs).

Catálogo: [docs/architecture/observability.md](docs/architecture/observability.md). IDE texto: `SPRING_PROFILES_ACTIVE=local`.

## Visão geral

Opt-in → agenda → criação com reserva/idempotência → worker SUBMIT/POLL → confirmação assíncrona. Conta de recebimento só muda após `CONFIRMADA`. Auditoria oficial = `operation_events` (não o log).

## Arquitetura (resumo)

`portability-api` + `central-simulator` + React; dois PostgreSQL; jobs com lease (sem broker). Ver [overview](docs/architecture/overview.md) e ADRs.

## Rodando sem Docker

Dois Postgres (plataforma `:5432`, Central `:5433`), `.env.example`, depois:

```bash
# Terminal 1 — Central
cd apps/central-simulator
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/central
export SPRING_DATASOURCE_USERNAME=central SPRING_DATASOURCE_PASSWORD=central
export CENTRAL_HMAC_SECRET=local-test-hmac-secret CENTRAL_HMAC_KEY_ID=local-test-key
export SERVER_PORT=8081
./mvnw spring-boot:run

# Terminal 2 — API
cd apps/portability-api
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/portability
export SPRING_DATASOURCE_USERNAME=portability SPRING_DATASOURCE_PASSWORD=portability
export CENTRAL_BASE_URL=http://localhost:8081
export CENTRAL_HMAC_SECRET=local-test-hmac-secret CENTRAL_HMAC_KEY_ID=local-test-key
export SERVER_PORT=8080
./mvnw spring-boot:run

# Terminal 3 — UI
cd apps/web && pnpm install && pnpm dev
```

Variáveis: [`.env.example`](.env.example).

## Dados de demonstração (sintéticos)

Inventário canônico (demos/E2E/k6): ver [docs/architecture/seed-inventory.md](docs/architecture/seed-inventory.md).

| Beneficiário | Parcelas | Uso |
| --- | --- | --- |
| `ben-ui` / `conv-1` | `parcela-ui-1..4` | Console / Playwright |
| `ben-001` / `conv-1` | `parcela-seed-1..2`, `parcela-load-01..20` | E2E dual-stack / k6 |
| `ben-recover` / `conv-1` | `parcela-recover-1..2` | Recuperação / restart |
| Sufixos `*-reject`, `*-503`, `*-timeout` | parcelas de falha + sufixo | Falhas **determinísticas** na Central |

**Clean seed não está disponível** como migration/script executável. `V11` é só marcador documental (no-op). `parcela-seed-3`/`ben-002` podem existir no histórico `V2` e não entram no inventário canônico.

Após uma confirmação, **não reabra parcialmente** parcelas `PORTED`. Em lab descartável: `docker compose down -v`. Sem wipe, parcelas OPEN extras (não reabre PORTED): `./scripts/lab-seed-extra-ui-parcels.sh`.

### Flyway V8 / V9 / V11 (Central)

- `V8` historicamente reabriu parcelas de demo no migrate e ainda roda em instalação limpa.
- `V9` / `V11` são marcadores de política (no-op); **não** apagam nem reabrem seeds.
- Reset seguro = volumes descartáveis ou `scripts/lab-reset-demo-parcels.sh`.

## Como demonstrar

### Cenário A — Portabilidade confirmada

1. Abra http://localhost:5173  
2. Autorize `ben-ui` / `conv-1`  
3. Selecione parcela OPEN → conta destino → revisão → envio  
4. Timeline até **Confirmada**; conta só então é efetivada  

### Cenário B — Rejeição de negócio

1. Beneficiário com sufixo `-reject` **ou** header `X-Sim-Fault: REJECT`  
2. Timeline termina **Rejeitada**; conta efetiva anterior preservada  

### Cenário C — Falha técnica (não é rejeição)

1. Sufixos `-503` / timeout, `CENTRAL_FAULT_RATE` em demo, ou Central indisponível  
2. Operação permanece não terminal / `REQUIRES_REVIEW`  
3. Worker faz retry; sem `REJEITADA` falsa  

### Cenário D — Idempotência

1. Mesmo payload + mesma `Idempotency-Key` → mesmo `operationId`  
2. Mesma chave + payload diferente → HTTP 409  
3. Disputa de **duas abas** pela mesma parcela (chaves diferentes) → um `202` e um `409` (nunca `500`)

## Falhas na Central: dois modos

| Modo | Como ativar | Uso |
| --- | --- | --- |
| Determinístico | Sufixo do beneficiário (`-reject`, `-503`, `-timeout`) ou header `X-Sim-Fault` | CI, dual-stack, demos reproduzíveis |
| Probabilístico (opcional) | `CENTRAL_FAULT_RATE=0.2` + `CENTRAL_FAULT_SEED=42` | Simular “falhas ocasionais” do enunciado |

Precedência: header → sufixo → sorteio. O padrão é `CENTRAL_FAULT_RATE=0` para a esteira permanecer determinística.

## Assinatura HMAC (headers, não no corpo)

O esboço do PDF sugere `assinatura` no JSON. Aqui a assinatura vai nos headers (`X-Signature`, `X-Timestamp`, `X-Nonce`, `X-Key-Id`): o corpo permanece só negócio e evita assinar um documento que contém a própria assinatura. Detalhes: [ADR-009](docs/adr/ADR-009-hmac.md).

## Como rodar os testes

Comandos Maven detalhados (clean, `-Dtest`, package, `spring-boot:run`): [docs/dev/maven.md](docs/dev/maven.md).

```bash
cd apps/portability-api && ./mvnw -B verify && cd ../..
cd apps/central-simulator && ./mvnw -B verify && cd ../..
cd apps/web && pnpm install && pnpm test && pnpm build && cd ../..

npx --yes @redocly/cli@1.34.5 lint --config redocly.yaml \
  contracts/openapi/platform.yaml contracts/openapi/central.yaml

# Compose saudável
./tests/e2e/dual-stack.sh
./tests/e2e/timeout-recovery.sh
./tests/e2e/process-kill-recovery.sh

cd tests/e2e && pnpm install && pnpm exec playwright install chromium && pnpm test && cd ../..
```

Cobertura de recuperação:

| Script | Cobre | Não cobre |
| --- | --- | --- |
| `timeout-recovery.sh` | Protocolo local perdido após aceite; mesma chave restaura | Morte exata no meio do HTTP |
| `process-kill-recovery.sh` | SIGKILL **depois** de persistir `central_protocol` | Janela crítica: Central aceitou e protocolo local ainda não durável |

Mitigação da janela pré-persistência: reutilizar a mesma `Idempotency-Key` + [runbook de reconciliação](docs/runbooks/reconciliation.md).

## Como a Central funciona

Serviço Spring Boot independente, banco próprio. Contrato sugerido (`/autorizacoes`, agenda, `202` + protocolo, status com `204` + `Retry-After`). Assina/verifica HMAC, idempotência por **hash do corpo bruto** (enquanto a plataforma usa fingerprint **semântico** — [ADR-005](docs/adr/ADR-005-idempotency.md)), reserva parcelas, atrasa confirmação e injeta falhas. A plataforma **não** segura transação de banco aberta durante chamadas HTTP à Central.

Divergências conscientes do esboço PDF (ex.: agenda `OPEN` vs `ABERTA`, conta achatada na borda): [contract-divergences.md](docs/architecture/contract-divergences.md).

### Status técnico vs negócio

- Negócio: `PENDENTE` / `CONFIRMADA` / `REJEITADA`.
- Job: `FAILED` = falhou com **retry** ainda elegível; `EXHAUSTED` = retries esgotados.
- `processing.status = REQUIRES_REVIEW` na API é **derivado** de `EXHAUSTED` (não é coluna de negócio).

## O que faria com mais tempo

- Autenticação do operador e controle de acesso por operação
- Rotação de chave HMAC e mTLS entre plataforma e Central
- Outbox com broker quando o volume justificar
- Workers em lote (claim N jobs por ciclo)
- Painel de reconciliação para `REQUIRES_REVIEW`
- Política de retenção de dados pessoais / PII
- Estudos de capacidade (soak) além do smoke k6 de laboratório

## Limitações conhecidas

- Laboratório: sem bancos reais, sem SSO de operador
- `V8` permanece no histórico Flyway da Central; `V9` não a desfaz
- Kill de processo cobre pós-persistência do protocolo; janela pré-persistência é residual documentado
- Acessibilidade automatizada é smoke (axe + teclado), não certificação WCAG 2.2 AA integral
- Contadores Prometheus são operacionais — fonte contábil = Postgres + trilha de auditoria

## Decisões arquiteturais

Comece pelo Miro no [mapa 00](https://miro.com/app/board/uXjVEJ1cEDw=/?share_link_id=843643210573&moveToWidget=3458764691629361954) e pelo índice em [system-design.md](docs/architecture/system-design.md). ADRs em `docs/adr/`. Detalhes: [idempotência](docs/architecture/idempotency-and-integrity.md), [lease do worker](docs/architecture/worker-lease.md), [observabilidade](docs/architecture/observability.md), [modelo de dados](docs/architecture/data-model.md), [Maven](docs/dev/maven.md).

## Bibliotecas externas utilizadas e justificativas

Somente dependências de produto/teste relevantes ao case (não lista transitivas do Spring Boot). Fontes: `apps/*/pom.xml`, `apps/web/package.json`, `tests/e2e/package.json`, `redocly.yaml`.

### Backend (portability-api e central-simulator)

| Biblioteca | Por quê |
| --- | --- |
| Spring Boot (Web, Validation, JDBC, Actuator) | HTTP + validação + SQL sem ORM; health/métricas para laboratório e Compose |
| Flyway (+ flyway-database-postgresql) | Migrações versionadas e reprodutíveis nos dois Postgres |
| PostgreSQL JDBC | Persistência tipada (`NUMERIC`/`timestamptz`); sem floating point monetário |
| Micrometer Prometheus | Contadores RED/USE scrapeáveis; fonte contábil continua sendo o banco + auditoria |
| Spring OpenTelemetry | Traces OTLP correlacionados API↔Central no stack LGTM local |
| JUnit 5 (via spring-boot-starter-test) | Testes de domínio, integração e contratos HTTP |
| Testcontainers (JUnit + PostgreSQL) | Postgres real nos testes de integração (Flyway, reserva, lease) |
| ArchUnit | Guarda limites hexagonais (domínio sem Spring/JPA/HTTP) |

### Frontend (`apps/web`)

| Biblioteca | Por quê |
| --- | --- |
| React 19 + Vite + TypeScript | Console tipado, build rápido, HMR no Compose |
| @tanstack/react-query | Cache/refetch do acompanhamento assíncrono sem estado global ad hoc |
| Vitest + Testing Library + jsdom | Testes de fluxo/UI no Node, alinhados ao comportamento do operador |
| oxlint | Lint rápido no CI local sem ESLint monolítico |

### Contratos, E2E e qualidade

| Biblioteca / ferramenta | Por quê |
| --- | --- |
| Redocly CLI | Lint OpenAPI da plataforma e da Central no CI |
| Playwright | E2E do console contra stack Compose |
| @axe-core/playwright | Smoke de acessibilidade (não certificação WCAG integral) |

Stack de observabilidade local (Grafana/Tempo/Loki via `otel-lgtm`, Prometheus scrape) está no Compose; não são libs da aplicação.

## Harness de IA

O desafio pede uso auditável de IA. Skills em `.agents/skills/`; evidências em `ai/sessions/`. Humanos aprovam commits e invariantes financeiras. Exemplos de correção/rejeição de sugestões da IA: [ai/README.md](ai/README.md).

## Evidência do tip publicado

| Campo | Valor |
| --- | --- |
| SHA (tip) | _(preenchido após commit — ver `git rev-parse HEAD`)_ |
| CI no tip | Pendente Actions após push deste pacote (docs alignment + fix e2e/contraste). Tip anterior `bef4234` [FAIL compose-smoke](https://github.com/mrdouglasmorais/Payroll-Portability-Platform---Case/actions/runs/45940063545). |
| Última CI verde conhecida | `d0dcbbc4f54b315e1f59008f87b81d4781c2c5d6` — [SUCCESS 8/8](https://github.com/mrdouglasmorais/Payroll-Portability-Platform---Case/actions/runs/45740557504) |

Não declare CI verde para commits ainda não publicados **nem** para tip com Actions vermelho.
