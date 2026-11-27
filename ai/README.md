# Harness de IA

Catálogo de skills: `.agents/skills/` (33). Instruções do projeto: `AGENTS.md`. Skills são procedimentos, não agentes autônomos.

Use `sessions/TEMPLATE.md` para evidência. Sessões em `sessions/` (`AI-000` … `AI-023`).

## Defesa no code review (exemplos concretos)

A IA acelera implementação; a revisão humana fecha invariantes financeiras. Exemplos em que sugestões foram **rejeitadas ou corrigidas**:

1. **Reabrir parcelas PORTED via migration/script “de reset”** — rejeitado. V8 ficou histórica; script de lab passou a **recusar** reopen; reset = volumes descartáveis.
2. **Kill de processo vendido como cobertura da janela crítica** — corrigido na narrativa. O harness cobre SIGKILL **depois** de persistir o protocolo; a janela Central-aceite → persistência local permanece residual mitigado por idempotência.
3. **Contadores financeiros dentro da transação** — corrigido com `afterCommit` (rollback não pode inflar métrica).
4. **Disputa de parcela retornando 500/400** — falha que passou por geração assistida e por testes que só contavam “exception” sem assert HTTP. Corrigido com `reserveIfOpen` + mapeamento **409**, e teste MockMvc concorrente (duas chaves, mesma parcela).
5. **Co-authored-by: Cursor no commit** — removido; tip deve ser só identidade do candidato.

Não trate narrativa de sessão como prova sem comando executado. Humanos aprovam commits e mudanças de invariante financeira.

## Sessões registradas

| ID | Tema |
|---|---|
| AI-000 | Fundação |
| AI-001 | Bootstrap |
| AI-002 | Domínio / persistência |
| AI-003 | Central |
| AI-004 | Núcleo de portabilidade |
| AI-005 | Console web |
| AI-005b | System design / Miro |
| AI-006 | Qualidade |
| AI-007 | Recuperação de falhas |
| AI-008 | Integridade financeira |
| AI-009 | Correção financeira |
| AI-010 | Endurecimento residual |
| AI-011 | UI pt-BR |
| AI-012 | Prontidão de release |
| AI-013 | Observabilidade + validação manual |
| AI-014 | Hardening final de release |
| AI-015…020 | Observabilidade, load, validação |
| AI-021 | README bibliotecas |
| AI-022 | RF4/RF5 conta + histórico |
| AI-023 | Paginação do histórico |
| AI-024 | Alinhamento docs vs código (matriz/ADRs) |

Nunca publique segredos ou dados reais de beneficiários.
