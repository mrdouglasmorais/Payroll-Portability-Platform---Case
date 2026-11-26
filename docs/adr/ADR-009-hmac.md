# ADR-009: HMAC em headers (não no corpo)

Status: Accepted

## Context

O contrato sugerido do desafio ilustra um campo `assinatura` no corpo JSON. Assinar um documento que contém a própria assinatura exige excluir ou zerar esse campo antes do cálculo — fonte clássica de divergência entre emissores e verificadores.

## Decision

Assinatura **fora** do corpo, em headers:

| Header | Papel |
|---|---|
| `X-Key-Id` | Identificador da chave HMAC |
| `X-Timestamp` | Relógio do emissor (skew máximo configurável) |
| `X-Nonce` | Uso único (anti-replay) |
| `X-Signature` | HMAC-SHA256 do canônico (método, path, timestamp, nonce, key id, corpo bruto, …) |
| `Idempotency-Key` | Identidade da intenção (separada da integridade) |

O corpo JSON permanece payload de negócio puro. O canônico inclui o **corpo bruto** (bytes), não um campo embutido.

## Alternative

Campo `assinatura` no JSON (como no esboço do PDF), com exclusão pré-canônica.

## Consequences

- Compatível em espírito com o requisito de integridade do desafio; diverge deliberadamente da forma ilustrativa do PDF.
- Evita recursão assinatura-dentro-do-payload.
- Produção ainda precisaria de TLS/mTLS e rotação de chaves (fora do lab).

## Revisit

Se um contrato externo exigir assinatura no corpo, adaptar o canônico com exclusão explícita documentada — não misturar os dois modelos.
