# Inventário canônico de seeds (Central)

**Author:** Douglas Morais  
**Policy:** documentação apenas — **não** há migration/script de “clean seed” disponível para operadores. Flyway `V11` é marcador no-op (mesmo padrão de `V9`). Não reescreva seeds históricos (`V2`–`V10`).

Se um volume lab antigo aplicou uma versão anterior de `V11` (com DELETE) e o Flyway acusar checksum mismatch: `docker compose down -v` (lab descartável) e suba de novo.

## Inventário usado por demos / E2E / k6

| Beneficiário | Parcelas | Uso |
| --- | --- | --- |
| `ben-ui` / `conv-1` | `parcela-ui-1..4` | Console / Playwright |
| `ben-001` / `conv-1` | `parcela-seed-1..2`, `parcela-load-01..20` | E2E dual-stack / k6 |
| `ben-recover` / `conv-1` | `parcela-recover-1..2` | Recuperação / restart |
| Sufixos `*-reject`, `*-503`, `*-timeout` | parcelas de falha + sufixo | Falhas determinísticas |

## Legacy no histórico Flyway (não usar em demos)

`V2` ainda insere `parcela-seed-3` / `ben-002`. Não fazem parte do inventário canônico acima (nenhum demo/E2E/k6 referencia). **Não** rode DELETE ad-hoc em volumes persistentes; em lab descartável use `docker compose down -v`.

## Reset

- Preferir beneficiários exclusivos por corrida (`ben-ui`, `ben-recover`).
- Reset estrutural de demo: `scripts/lab-reset-demo-parcels.sh` (explícito — hoje recusa reopen financeiro).
- Volumes: `docker compose down -v` (lab apenas).

## Dual-DB (platform vs Central)

`authorizations` na Central **não** são seed Flyway — nascem em runtime via `POST /autorizacoes`.  
Se só o volume `central-db` for recriado, consents `ACCEPTED` no platform ficam órfãos → agenda **403**.

Heal (lab):

1. `POST /api/v1/autorizacoes` de novo (a API reafirma na Central mesmo com consent local), ou
2. `./scripts/lab-sync-central-authorizations.sh`, ou
3. `docker compose down -v && docker compose up --build -d`
