# AI harness

Canonical skill catalog: `.agents/skills/` (33 skills). `AGENTS.md` defines project instructions. Explicitly load relevant skills if the agent does not discover them automatically. Skills are instructions, not autonomous agents. No runtime orchestration is required.

Use `sessions/TEMPLATE.md` for actual prompts and evidence.

## Recorded sessions

| ID | Topic |
|---|---|
| AI-000 | Foundation |
| AI-001 | Project bootstrap |
| AI-002 | Domain persistence |
| AI-003 | Central simulator |
| AI-004 | Portability core |
| AI-005 | Web console |
| AI-005b | System design doc / Miro |
| AI-006 | Quality hardening |
| AI-007 | Failure recovery |
| AI-008 | Financial integrity |
| AI-009 | Financial correctness |
| AI-010 | Residual hardening |
| AI-011 | Operator UI pt-BR |
| AI-012 | Release readiness audit |
| AI-013 | Observability hardening + manual validation DONE |
| AI-014 | Final release hardening (docs, afterCommit metrics, a11y smoke) |

Never publish secrets or real beneficiary data. Human owns final decisions. Independent review is a separate pass, not proof of correctness. Historical session files are preserved as written.
