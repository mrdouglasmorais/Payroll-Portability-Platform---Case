# ADR-006: persistent-jobs

Status: Accepted

## Context
Technical challenge requires durable async work without holding DB transactions across HTTP calls to the Central.

## Decision
Transactional job table with lease, fencing token, `next_run_at`, and technical statuses.

### Technical statuses (naming)

| Status | Meaning |
| --- | --- |
| `PENDING` | Aguardando claim |
| `RUNNING` | Lease ativo |
| `FAILED` | Última tentativa falhou; **ainda há retry** (`next_run_at` no futuro) — o job continua elegível a claim |
| `SUCCEEDED` | Trabalho técnico concluído |
| `EXHAUSTED` | Retries esgotados; API deriva `processing.status=REQUIRES_REVIEW` (não é rejeição de negócio) |

`FAILED` ≠ terminal financeiro. Não confundir com `EXHAUSTED` / `REQUIRES_REVIEW`.

## Alternative
Kafka or in-memory scheduler.

## Consequences
Atomic enqueue with operation create; polling overhead; ops must distinguish retryable `FAILED` from `EXHAUSTED`.

## Revisit
Broker/outbox when volume justifies (see README “mais tempo”).
