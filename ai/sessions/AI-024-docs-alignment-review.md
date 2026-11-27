# AI-024 — Alinhamento documentação vs código (review)

**Date:** 2026-11-26  
**Author:** Douglas Morais (via Cursor agent)  
**Skills:** documentation, ai-governance, api-contract-design  

## Prompt (redacted)

Revisão apontou incongruências docs↔código (SPECs citadas, SHA/CI, ADR-005, ADRs Proposed, REQUIRES_REVIEW, divergências PDF). Preparar correções.

## Constraints

- Honestidade de CI: tip `bef4234` FAIL compose-smoke; este pacote inclui o fix; não inventar CI verde até Actions SUCCESS.
- SPECs 007–009 já existiam no tip; marcar Accepted e referenciar.

## Changes / files

- `docs/adr/ADR-005` — semantic (plataforma) vs raw-body (Central)
- `docs/adr/ADR-006/007` — FAILED vs EXHAUSTED; REQUIRES_REVIEW derivado; RF06→ADR-007
- ADRs 001–013 + SPECs Proposed → Accepted
- `docs/architecture/contract-divergences.md` — OPEN/ABERTA, conta achatada, HMAC, status
- `docs/compliance-matrix.md`, `README.md`, `docs/acceptance.md`, `ai/README.md`
- Fix CI: `operator-flow.spec.ts` + contraste Pendente `#9a5500`

## Suggestions rejected and why

- Marcar painel de histórico como PARTIAL: já entregue (AI-022/023); residual só auth lab + CI tip.
- Inventar que SPECs 007–009 faltavam no tip atual: arquivos já estavam em `docs/specs/`.

## Commands and actual outputs

```text
$ ls docs/specs
SPEC-001 … SPEC-009 present

$ git log -1 --format='%H %ci'
bef4234058efa54bc282b7f416ba0e2e4c9b2d2b 2026-11-26 15:09:37 -0300
```

## Validation

Docs alinhados. Fix e2e/contraste incluído neste commit. Playwright/CI re-run: NOT_RUN até push + Actions.

## Human review / decision

Candidate approved commit+push of alignment package.

## Remaining risks

- Avaliador pode ter visto tip anterior sem SPEC-007…009 — `bef4234` já as continha.
- CI tip permanece PARTIAL até Actions verde no SHA publicado.
