# Plataforma de Portabilidade de Folha

Simulação de portabilidade de descontos em folha (Java/Spring Boot + React/TypeScript + PostgreSQL) com um simulador independente da **Central de Convênios**. Apenas dados sintéticos — sem tráfego financeiro real.

Fonte de decisões de arquitetura: [board Miro](https://miro.com/app/board/uXjVEJ1cEDw=/?share_link_id=843643210573) ([docs/architecture/system-design.md](docs/architecture/system-design.md)). Rastreio: [matriz de conformidade](docs/compliance-matrix.md), [aceitação](docs/acceptance.md), [contratos OpenAPI](contracts/openapi/), [harness de IA](ai/README.md), [AGENTS.md](AGENTS.md).

## Visão geral

O console do operador registra o opt-in do beneficiário, carrega a agenda autorizada, cria a portabilidade (uma ou várias parcelas) com conta de destino, acompanha a confirmação assíncrona da Central e exibe a linha do tempo append-only. A conta de recebimento só é efetivada após `CONFIRMADA` verificada.

## Funcionalidades

- Autorização (opt-in) com identificador único via Central
- Agenda futura autorizada (valor, data de desconto, convênio, status)
- Criação com elegibilidade, reserva, idempotência
- Fluxo assíncrono 202 + protocolo + worker SUBMIT/POLL (retry, lease, fencing)
- Eventos append-only da operação
- Distinção rejeição de negócio × falha técnica (`REQUIRES_REVIEW`)
- Integridade HMAC nos dois sentidos; injeção de falhas na Central

## Arquitetura (resumo)

Monólito modular (`portability-api`) + Central independente + UI React. Dois PostgreSQL. Jobs persistidos com lease (sem broker). Transações locais; consistência eventual via poll/reconciliação. Ver [overview](docs/architecture/overview.md) e ADRs em `docs/adr/`.

## Pré-requisitos

- Docker + Docker Compose (caminho recomendado), **ou**
- Temurin 25, Node 24, pnpm 10.18.2, PostgreSQL 17 local (dois bancos)

## Como executar (Docker)

```bash
git clone https://github.com/mrdouglasmorais/Payroll-Portability-Platform---Case.git
cd Payroll-Portability-Platform---Case
cp .env.example .env
docker compose up --build -d
```

| Superfície | URL |
| --- | --- |
| Console operacional (pt-BR) | http://localhost:5173 |
| API da plataforma (health) | http://localhost:8080/actuator/health |
| Status da plataforma | http://localhost:8080/api/v1/status |
| Métricas Prometheus (API) | http://localhost:8080/actuator/prometheus |
| Central (health) | http://localhost:8081/actuator/health |

Parar: `docker compose down`. Lab descartável (apaga volumes): `docker compose down -v`.

## Rodando sem Docker

Use dois Postgres (plataforma e Central), copie `.env.example` e ajuste URLs/portas.

### 1) Central de Convênios (mock)

```bash
cd apps/central-simulator
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/central
export SPRING_DATASOURCE_USERNAME=central
export SPRING_DATASOURCE_PASSWORD=central
export CENTRAL_HMAC_SECRET=local-test-hmac-secret
export CENTRAL_HMAC_KEY_ID=local-test-key
export CENTRAL_CONFIRM_DELAY=PT2S
export CENTRAL_FAULT_RATE=0          # demos ocasionais: 0.2
export CENTRAL_FAULT_SEED=42
export SERVER_PORT=8081
./mvnw spring-boot:run
```

Health: `http://localhost:8081/actuator/health`

### 2) Backend da plataforma

```bash
cd apps/portability-api
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/portability
export SPRING_DATASOURCE_USERNAME=portability
export SPRING_DATASOURCE_PASSWORD=portability
export CENTRAL_BASE_URL=http://localhost:8081
export CENTRAL_HMAC_SECRET=local-test-hmac-secret
export CENTRAL_HMAC_KEY_ID=local-test-key
export SERVER_PORT=8080
./mvnw spring-boot:run
```

Health: `http://localhost:8080/actuator/health`

### 3) Frontend (console)

```bash
cd apps/web
pnpm install
pnpm dev
```

Dev server em http://localhost:5173 (proxy de `/api` e `/actuator` para a API em `:8080`).

Variáveis principais também estão em [`.env.example`](.env.example).

## Dados de demonstração (sintéticos)

| Beneficiário | Uso |
| --- | --- |
| `ben-ui` / `conv-1` | Console / Playwright (`parcela-ui-*`) |
| `ben-001` | E2E dual-stack |
| `ben-recover` | Recuperação / restart |
| Sufixos `*-reject`, `*-503`, `*-timeout` | Falhas **determinísticas** na Central |

Após uma confirmação, **não reabra parcialmente** parcelas `PORTED`. Em lab descartável: `docker compose down -v`.

### Flyway V8 / V9 (Central)

- `V8` historicamente reabriu parcelas de demo no migrate e ainda roda em instalação limpa.
- `V9` é marcador de política (no-op) e **não desfaz** a V8.
- Reset seguro = volumes descartáveis; não use isso em ambientes financeiros persistentes.

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

Serviço Spring Boot independente, banco próprio. Contrato sugerido (`/autorizacoes`, agenda, `202` + protocolo, status com `204` + `Retry-After`). Assina/verifica HMAC, idempotência (fingerprint do corpo bruto), reserva parcelas, atrasa confirmação e injeta falhas. A plataforma **não** segura transação de banco aberta durante chamadas HTTP à Central.

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

Ver `docs/adr/` (monólito modular, dual DB, jobs persistidos, HMAC, reservas, polling, harness de IA). Detalhes: [idempotência](docs/architecture/idempotency-and-integrity.md), [lease do worker](docs/architecture/worker-lease.md), [observabilidade](docs/architecture/observability.md).

## Harness de IA

O desafio pede uso auditável de IA. Skills em `.agents/skills/`; evidências em `ai/sessions/`. Humanos aprovam commits e invariantes financeiras. Exemplos de correção/rejeição de sugestões da IA: [ai/README.md](ai/README.md).

## Evidência do tip publicado

| Campo | Valor |
| --- | --- |
| SHA | `d0dcbbc4f54b315e1f59008f87b81d4781c2c5d6` |
| CI remota | [SUCCESS 8/8](https://github.com/mrdouglasmorais/Payroll-Portability-Platform---Case/actions/runs/45740557504) |

Não declare CI verde para commits ainda não publicados.
