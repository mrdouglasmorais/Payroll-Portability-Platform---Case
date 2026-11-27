# Divergências do esboço PDF (documentadas)

O PDF do desafio é ilustrativo e de contrato aberto. Este laboratório diverge de propósito onde o desenho melhora integridade ou clareza. HMAC já está em [ADR-009](../adr/ADR-009-hmac.md).

## Agenda: `ABERTA` (PDF) vs `OPEN` (implementação)

| Camada | Valores |
| --- | --- |
| PDF (exemplo) | `ABERTA` (e afins em português) |
| Central / snapshots | `OPEN`, `RESERVED`, `PORTED`, `CLOSED` |
| Operação de negócio | `PENDENTE`, `CONFIRMADA`, `REJEITADA` |

Motivo: inglês estável no protocolo HTTP/SQL da Central; português nos status de negócio expostos ao operador. O console traduz labels na UI.

## Conta de recebimento: objeto vs campos achatados

| Fluxo | Forma |
| --- | --- |
| PDF / API plataforma (create) | `contaRecebimento: { banco, agencia, conta }` |
| Submissão plataforma → Central | Campos achatados `banco` / `agencia` / `conta` no JSON do protocolo |

Motivo: contrato sugerido da Central no case usa campos no root do body; a plataforma mantém o objeto aninhado na API do operador e achata só na borda HMAC.

## Idempotência: fingerprint semântico vs corpo bruto

Ver [ADR-005](../adr/ADR-005-idempotency.md). Plataforma = hash semântico; Central = SHA-256 dos bytes do body. Emissores externos à plataforma não são suportados neste lab.

## Assinatura

PDF sugere `assinatura` no JSON. Implementação usa headers HMAC (`X-Signature`, `X-Timestamp`, `X-Nonce`, `X-Key-Id`) — [ADR-009](../adr/ADR-009-hmac.md).

## Status técnico `REQUIRES_REVIEW`

Não é coluna/enum de negócio. Em `GET .../operacoes-portabilidade/{id}`, `processing.status = REQUIRES_REVIEW` é **derivado** quando o job persistido está `EXHAUSTED` (`PortabilityController`). Negócio permanece `PENDENTE` até confirmação/rejeição verificada.

## Job `FAILED` vs `EXHAUSTED`

| Technical status | Significado |
| --- | --- |
| `FAILED` | Tentativa falhou; job **continua elegível** a claim com `next_run_at` (retry/backoff). |
| `EXHAUSTED` | Retries esgotados → API deriva `REQUIRES_REVIEW`; não é rejeição financeira. |
