# System design (documento balizador)

**Fonte autoritativa de decisões de arquitetura:** board Miro  
[Payroll Portability — System Design](https://miro.com/app/board/uXjVEJ1cEDw=/?share_link_id=843643210573)

Este board foi o **documento balizador** das tomadas de decisão do case. Specs (`docs/specs/`), ADRs (`docs/adr/`), contratos OpenAPI e o código devem permanecer alinhados a ele. Em caso de divergência, tratar o Miro como intenção de design e registrar a resolução (ADR + sessão AI + revisão humana).

Cópia local resumida abaixo (índice + decisões-chave extraídas do board). Diagramas detalhados, teses de defesa e atributos de qualidade vivem no Miro.

## Índice do board (20 frames)

| # | Frame | Tema |
|---|---|---|
| 01 | Contexto de negócio e requisitos | Escopo, RF/RNF, dados sintéticos |
| 02 | Contexto do sistema · C4 L1 | Plataforma vs Central |
| 03 | Arquitetura de containers · C4 L2 | Web, API, Central, 2× PostgreSQL |
| 04 | Arquitetura de componentes · C4 L3 | Domínio independente de framework |
| 05 | Arquitetura de domínio · DDD | Bounded contexts / módulos |
| 06 | Arquitetura de dados · ERD | Reservas, idempotência, eventos |
| 07 | Ciclo de vida da portabilidade | Opt-in → 202 → worker → protocolo → status |
| 08 | Idempotência e concorrência | Chave + hash; 409; reserva única |
| 09 | Processamento assíncrono e recuperação | Timeout/503 ≠ rejeição; lease |
| 10 | Máquina de estados da operação | PENDENTE → CONFIRMADA / REJEITADA |
| 11 | Arquitetura de segurança | HMAC, segredos, mascaramento |
| 12 | Observabilidade e confiabilidade | Logs, traces, métricas, SLO ilustrativo |
| 13 | Infraestrutura e DevSecOps | Compose, CI, sem alegação produtiva |
| 14 | Harness de engenharia com IA | Skills, evidência, aprovação humana |
| 15 | Decisões arquiteturais e trade-offs | Monólito, jobs DB, polling, FMEA |
| 16 | Teses e defesa: arquitetura | Jobs duráveis + polling |
| 17 | Teses e defesa: consistência | Timeout não prova falha financeira |
| 18 | Atributos de qualidade | Segurança, manutenibilidade |
| 19 | Atributos de qualidade operacionais | Disponibilidade e resiliência |
| 20 | Roteiro de defesa técnica | Narrativa de avaliação |

## Decisões-chave (extraídas do Miro)

### Negócio e aceite (frame 01)

- Fluxo: opt-in → seleção de parcelas → registro → Central confirma/rejeita → conta só muda após confirmação.
- Funcionais: autorização, agenda, seleção, registro, status, auditoria.
- Não funcionais: HMAC, idempotência nos dois sistemas, polling, retry e falhas parciais.
- Restrições: Central simulada, dados sintéticos, README reproduzível; **sem integração bancária real**; uma solicitação lógica → uma operação.

### Containers C4 L2 (frame 03)

```mermaid
flowchart LR
  Web["React / Nginx :5173"] --> Api["Portability API :8080"]
  Api --> PlatformDb[(Platform PostgreSQL)]
  Api -->|HTTP assinado| Central["Central simulator :8081"]
  Central --> CentralDb[(Central PostgreSQL)]
```

- Navegador acessa **somente** a plataforma.
- Bancos separados; sem transação compartilhada nem acesso cruzado direto.
- Stack alvo: Java 25, Spring Boot, JDBC, Flyway, React, Compose.

### Ciclo de vida (frame 07)

1. Opt-in + agenda  
2. POST local com chave estável  
3. Commit atômico de operação + reserva + evento + job  
4. Worker envia POST assinado à Central (**fora** da transação SQL)  
5. Central responde 202 + protocolo  
6. Poll de status → confirma/rejeita e audita  

Invariantes: retornar 202 após commit durável; UI consulta API local até terminal; rejeição preserva conta original; confirmar e atualizar atomicamente.

### Idempotência e concorrência (frame 08)

- Chave estável + hash canônico; reivindicação atômica / índice único.
- Mesmo payload → resposta original; payload diferente → **409**.
- Reserva ativa única por parcela; chaves distintas não competem pela mesma parcela ativa.
- Após aceite com resposta perdida: reenviar chave de negócio com **novo nonce**.
- Testes concorrentes em PostgreSQL real; retry limitado a deadlock, não a conflito de negócio.

### Trade-offs (frame 15)

| Escolha | Racional |
|---|---|
| Monólito modular + Central separada | Menos operação local; Central reproduz falhas de rede |
| Jobs no PostgreSQL (vs broker) | Agendamento atômico; broker só se métricas justificarem |
| Polling (vs webhooks) | Alinha ao contrato da Central |
| JDBC (vs ORM pesado) | Controle de invariantes financeiros |
| Auditoria append-only (vs event sourcing completo) | Custo/PII menores no escopo do case |

FMEA resumido: resposta perdida → dedupe; crash → lease; gravação obsoleta → fencing; assinatura inválida → rejeitar.

### Harness de IA (frame 14)

- Spec → skill → implementação → testes reais → revisão independente → evidência → **aprovação humana**.
- Nunca inventar validações; distinguir PASS / FAIL / NOT_RUN.
- Commits sob revisão humana (este repositório segue esse gate).

## Mapeamento para o repositório

| Miro | Artefatos locais |
|---|---|
| 01 requisitos | [requirements-traceability.md](../requirements-traceability.md), `docs/specs/*` |
| 02–05 C4/DDD | [overview.md](overview.md), ADR-001/002 |
| 06 dados | ADR-003, Flyway V2 |
| 07–10 async/estado | SPEC-003/004/007/008, ADR-006/007/010/011 |
| 08 idempotência | SPEC-005, ADR-005 |
| 11 segurança | SPEC-006, ADR-009, [hmac-canonical.md](hmac-canonical.md) |
| 13 DevSecOps | `.github/workflows`, `docker-compose.yml` |
| 14 IA | `AGENTS.md`, `ai/sessions/*`, ADR-015 |
| 15 trade-offs | ADRs + README Trade-offs |

## Governança

1. Alterações que mudem invariantes financeiros, de segurança ou de consistência devem citar o frame Miro afetado na sessão AI e no ADR.
2. Atualizar este resumo quando o board mudar de forma material.
3. O board permanece o artefato visual de defesa técnica; este arquivo é o índice versionado no Git.
