# AI-011 — UI e mensagens operador em pt-BR

- Task / spec / ADR: console operacional (SPEC-009 / ADR-012) — localização pt-BR
- Tool and model: Cursor agent (Composer)
- Skills loaded: product-design, react-engineering, frontend-architecture, ai-governance
- Actual prompt (redacted): texto da UI em português; análise completa e conclusão
- Constraints: não inventar resultados de teste; contratos de status/códigos permanecem estáveis; human owns commits

## Changes / files

- `apps/web/src/shared/i18n/labels.ts` (+ teste): rótulos pt-BR para status, eventos, códigos e mensagens legadas EN
- `apps/web/src/features/portability/PortabilityWizard.tsx`: exibe rótulos localizados; etapas capitalizadas; datas pt-BR
- `apps/web/src/shared/api/client.ts`: fallback de erro em pt-BR
- `apps/web` / `tests/e2e`: asserts em “Confirmada”
- `apps/portability-api`: mensagens de domínio/API/eventos/worker/gateway voltadas ao operador em pt-BR
- Central simulator: mantido em inglês (sistema externo simulado)

## Suggestions rejected and why

- i18n multi-idioma completo (i18next): fora de escopo; produto é pt-BR único
- Traduzir códigos de contrato (`PENDENTE`, `OPERATION_CREATED`) no wire: quebraria OpenAPI/clientes; só labels de UI

## Commands and actual outputs

```text
$ cd apps/web && pnpm test && pnpm exec tsc -b --pretty false
Test Files  5 passed (5)
Tests  9 passed (9)
```

- Java focused tests: ver saída do comando no turno (registrar PASS/FAIL abaixo)

## Validation

```text
$ cd apps/web && pnpm test && pnpm exec tsc -b --pretty false
Test Files  5 passed (5)
Tests  9 passed (9)

$ cd apps/portability-api && ./mvnw -q test -Dtest='PortabilityOperationTest'
tests="4" errors="0" skipped="0" failures="0"
```

- Validation: PASS (web); PASS (PortabilityOperationTest); IT com Testcontainers NOT_RUN (Docker indisponível no sandbox)
- Human review / decision: pending
- Commit reference: pending (user-executed)
- Remaining risks: eventos/erros já persistidos em EN em bancos existentes; mapa de legado cobre os principais. Logs internos e mensagens do simulador Central podem permanecer em EN.
