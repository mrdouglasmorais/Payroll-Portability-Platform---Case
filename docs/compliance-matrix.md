# Compliance matrix (release readiness)

Source of truth: original challenge PDF (local context only, not published) pp. 1–4; mapped IDs are local.
Status values: `PASS` | `PARTIAL` | `FAIL` | `NOT_RUN`.

Specs: `docs/specs/SPEC-001` … `SPEC-009` (all present). ADRs: `docs/adr/ADR-001` … `ADR-015`.
PDF divergences: [architecture/contract-divergences.md](architecture/contract-divergences.md).

## Evidence baseline

| Kind | Reference |
|---|---|
| Published tip (código) | _(este pacote AI-024 + fix compose-smoke — SHA preenchido após commit)_ |
| Remote CI on tip | Pendente Actions após push. Tip anterior `bef4234` [run 45940063545](https://github.com/mrdouglasmorais/Payroll-Portability-Platform---Case/actions/runs/45940063545) **FAIL** (`compose-smoke`). Este tip inclui locator scoped + contraste `#9a5500`. **Não** declarar CI verde até Actions verde. |
| Last known green CI tip | `d0dcbbc4f54b315e1f59008f87b81d4781c2c5d6` — [run 45740557504](https://github.com/mrdouglasmorais/Payroll-Portability-Platform---Case/actions/runs/45740557504) SUCCESS 8/8 |
| Sessions | `ai/sessions/AI-000` … `AI-024` |

| ID | Origem | Requisito | Obrigatoriedade | Spec | ADR | Implementação | Teste | Evidência | Status | Risco residual |
|---|---|---|---|---|---|---|---|---|---|---|
| RF01 | PDF p.1–2 | Opt-in / autorização com id único | Obrigatório | SPEC-001 | ADR-004 | Controllers + Central | `PortabilityApiWebTest`, dual-stack, Playwright | tip AI-024 + last green `d0dcbbc` | PASS | Sem auth de operador (lab) |
| RF02 | PDF p.2 | Agenda futura autorizada | Obrigatório | SPEC-002 | ADR-004 | `SyncAgendaService` | dual-stack, Playwright, agenda sync IT | tip AI-024 | PASS | Status agenda `OPEN` (não `ABERTA` do PDF) — documentado |
| RF03 | PDF p.2 | Seleção 1..N + conta + elegibilidade | Obrigatório | SPEC-003 | ADR-011 | `CreatePortabilityService` | create atomic, order, concurrent | tip AI-024 | PASS | Conta achatada na borda Central — documentado |
| RF04 | PDF p.2 | Async status; conta só na confirmação | Obrigatório | SPEC-004 | ADR-006/010 | Worker + Central + TrackingStep `contaEfetivada` | worker + recovery scripts + wizard | AI-022 / tip AI-024 | PASS | Kill pré-persistência do protocolo residual |
| RF05 | PDF p.2 | Trilha append-only + histórico | Obrigatório | SPEC-007 / SPEC-009 | ADR-008 | `operation_events` + `GET` paginado + HistoryPanel | dual-stack eventos, list IT, wizard | AI-022/023 / tip AI-024 | PASS | Sem auth de operador; CI tip pendente Actions |
| RF06 | PDF p.2 | Rejeição vs falha integração | Obrigatório | SPEC-008 | ADR-007 | Business vs technical states; `REQUIRES_REVIEW` derivado de `EXHAUSTED` | dual-stack reject, worker transient | tip AI-024 | PASS | — |
| RNF01 | PDF p.2 | HMAC | Obrigatório | SPEC-006 | ADR-009 | Filter + gateway | Central HMAC ITs | last green `d0dcbbc` | PASS | Lab HMAC ≠ TLS |
| RNF02 | PDF p.2 | Idempotência E2E | Obrigatório | SPEC-005 | ADR-005 | Platform semantic hash + Central raw-body hash | 409 / concurrent | tip AI-024 | PASS | Ordem JSON só é segura com emissor único (plataforma) |
| RNF03 | PDF p.2 | 202 + poll + backoff | Obrigatório | SPEC-004 | ADR-010 | Worker SUBMIT/POLL | timeout-recovery | tip AI-024 | PASS | — |
| RNF04 | PDF p.2 | 503/timeout/unexpected | Obrigatório | SPEC-008 | ADR-007 | Fault injection | worker + Central faults | tip AI-024 | PASS | — |
| RNF05 | PDF p.2 | Auditoria | Obrigatório | SPEC-007 | ADR-008 | Append events | fenced append | tip AI-024 | PASS | — |
| RNF06 | PDF p.2 | Manutenibilidade | Obrigatório | AGENTS | ADR-002 | Feature packages | ArchUnit | tip AI-024 | PASS | — |
| RNF07 | PDF p.2 | Testes críticos | Obrigatório | ADR-013 | ADR-013 | JUnit/Vitest/E2E/Playwright | suite + CI | last green `d0dcbbc`; tip anterior `bef4234` FAIL; tip AI-024 inclui fix | PARTIAL | CI tip pendente Actions — não declarar verde até run SUCCESS |
| RNF08 | PDF p.2–3 | Uso de IA | Obrigatório | ADR-015 | ADR-015 | sessions AI-000…024 | session files + human review | AI-014…024 | PASS | Narrativa ≠ prova sem comando |
| RNF09 | PDF p.4 | README + reproduzível | Obrigatório | — | ADR-001 | Compose + README | compose up + scripts | tip AI-024 | PASS | Confirmar Actions após cada push |
| Obs | ADR-014 | Métricas + correlation | Recomendado | ADR-014 | ADR-014 | Prometheus + MDC + afterCommit | telemetry IT | tip AI-024 | PASS | Métricas ≠ fonte contábil |

## Honestidade sobre o painel de histórico

Implementado (listagem paginada + HistoryPanel). Não marcar PARTIAL por ausência — a lacuna anterior foi fechada em AI-022/023. Residual: lab sem auth de operador; CI tip pendente após push do fix compose-smoke.
