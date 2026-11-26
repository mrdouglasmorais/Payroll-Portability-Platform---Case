# AI-022 — RF4 conta efetivada na UI + RF5 listagem/histórico

**Date:** 2026-11-26  
**Author:** Douglas Morais (via Cursor agent)  
**Skills:** api-contract-design, product-design, react-engineering, privacy-engineering, documentation, ai-governance, test-engineering  

## Prompt (redacted)

Ajustar gaps: RF4 UI (TrackingStep sem conta antes/depois) e RF5 (sem GET listagem nem painel de histórico).

## Constraints

- Conta mascarada nas respostas HTTP.
- `contaEfetivada` somente em `CONFIRMADA`.
- Sem commit até pedido explícito.

## Changes / files

- `contracts/openapi/platform.yaml` — GET collection + campos de conta no status
- Controller/DTOs/repositório JDBC
- `TrackingStep`, `HistoryPanel`, `usePortabilityFlow`
- `SPEC-009-web.md`, `compliance-matrix.md`
- Testes web + wizard

## Suggestions rejected and why

- Conta sem máscara: privacy / SPEC-009.
- Snapshot de conta anterior por parcela: não há histórico de `effective_*` na plataforma.

## Commands and actual outputs

```text
$ npx @redocly/cli@1.34.5 lint --config redocly.yaml contracts/openapi/platform.yaml
validated (warnings preexistentes)

$ cd apps/portability-api && ./mvnw -B -Dtest=PortabilityApiWebTest test
Tests run: 6 — BUILD SUCCESS

$ cd apps/web && pnpm test
15 passed (15)

$ cd apps/web && pnpm exec tsc -b
exit 0
```

## Validation

OpenAPI PASS · PortabilityApiWebTest PASS · Vitest PASS · tsc PASS · Playwright NOT_RUN

## Human review / decision

pending

## Remaining risks

- Listagem evoluiu para envelope paginado em AI-023.
- Playwright E2E NOT_RUN.
