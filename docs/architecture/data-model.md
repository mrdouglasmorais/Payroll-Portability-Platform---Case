# Modelo de dados (DBML)

**Author:** Douglas Morais ([mrdouglasmorais](https://github.com/mrdouglasmorais))  
Fonte autoritativa de schema: Flyway em `apps/*/src/main/resources/db/migration`.  
Arquivo DBML: [data-model.dbml](data-model.dbml).  
Workspace Notion: [Payroll Portability docs](https://app.notion.com/p/424e56450c38818bb71fc51de037a438?v=424e56450c388121ab7f000c7541fddc&t=424e56450c3880448a0200a9a19721d3).

## Diagrama publicado

[Abrir no dbdiagram.io](https://dbdiagram.io/e/6b07b78d6514799802110cd1/6b07b83365147998021115fe)

<iframe width="100%" height="480" src="https://dbdiagram.io/e/6b07b78d6514799802110cd1/6b07b83365147998021115fe" title="Payroll Portability DBML" style="border:0;border-radius:4px;"></iframe>

> Em visualizadores Markdown que bloqueiam iframe, use o link acima. Após editar `data-model.dbml`, reimporte no dbdiagram e atualize o embed se o ID mudar.

## Conformidade (verificado contra Postgres live)

| Item | Status |
|---|---|
| Tabelas domain platform (9) | Conformante |
| Tabelas domain central (7) | Conformante |
| Colunas + nullability | Conformante |
| Índices parciais de reserva / consent / claim | Documentados no DBML |
| `NUMERIC(19,2)` / `timestamptz` / `date` | Conformante |
| Links lógicos parcela + protocolo | Documentados (não são FKs) |
| `schema_bootstrap` / `flyway_schema_history` | Omitidos de propósito (infra) |
| Namespaces `platform.*` / `central.*` | Namespace lógico DBML — nos DBs reais as tabelas ficam em `public` |

CHECks de negócio (`status IN (...)`, `amount >= 0`, terminal + rejection_reason) existem no SQL e estão citados em Notes do DBML (DBML não emite CHECK constraints).

## Dual DB — os dois bancos estão no DBML?

**Sim.** O arquivo [data-model.dbml](data-model.dbml) documenta **os dois** PostgreSQL como namespaces lógicos:

| Namespace DBML | Compose / JDBC | Conteúdo |
| --- | --- | --- |
| `platform.*` | `platform-db` / DB `portability` | 9 tabelas de domínio (beneficiários → jobs) |
| `central.*` | `central-db` / DB `central` | 7 tabelas (agenda, protocolos, HMAC nonces, …) |

Sem FK cruzada (ADR-003). Links lógicos (`Ref:`):

- `platform.installment_snapshots.external_installment_id` ↔ `central.agenda_installments.external_installment_id`
- `platform.portability_operations.central_protocol` ↔ `central.portability_protocols.protocol`
- `platform.beneficiaries.external_ref` ↔ `central.agenda_installments.beneficiary_id`

`correlation_id` é operacional (MDC / `X-Correlation-Id` / `operation_events`), não FK.
