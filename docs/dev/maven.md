# Comandos Maven (Java 25)

Cada serviço tem **Maven Wrapper** próprio (`apps/*/mvnw`). Não há POM multi-módulo na raiz. JDK alvo: **Temurin 25** (CI e Dockerfiles).

## Portability API

```bash
cd apps/portability-api

./mvnw -B verify                 # testes + package (CI)
./mvnw -B clean verify           # limpa target/ antes
./mvnw -B test                   # só unit/IT
./mvnw -B -Dtest=ArchitectureTest,PortabilityTelemetryCommitTest test
./mvnw -B -DskipTests package    # jar sem testes
./mvnw spring-boot:run           # sobe em :8080 (precisa Postgres plataforma)
```

Variáveis típicas: ver `.env.example` e README raiz (seção *sem Docker*).

## Central simulator

```bash
cd apps/central-simulator

./mvnw -B verify
./mvnw -B clean verify
./mvnw -B test
./mvnw -B -Dtest=CentralHmacIntegrationTest,FaultSelectorTest test
./mvnw -B -DskipTests package
./mvnw spring-boot:run           # sobe em :8081 (precisa Postgres central)
```

## Da raiz do repositório

```bash
./apps/portability-api/mvnw -B -f apps/portability-api/pom.xml verify
./apps/central-simulator/mvnw -B -f apps/central-simulator/pom.xml verify
```

CI usa o mesmo padrão (`./mvnw -B verify` dentro de cada app directory).

## Pré-requisitos de teste

| Necessidade | Motivo |
|---|---|
| Docker disponível | Testcontainers (PostgreSQL real nos ITs) |
| Java 25 | `java.version` nos POMs |
| Sem containers API nas portas 8080/8081 | Conflito se for rodar `spring-boot:run` local |

## O que não documentar como “Maven”

Frontend: `pnpm` em `apps/web`. E2E: `tests/e2e`. Carga: `./scripts/run-k6-m6.sh`. OpenAPI: Redocly CLI (README raiz).
