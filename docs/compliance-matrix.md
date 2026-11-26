# Compliance matrix (release readiness)

Source of truth: original challenge PDF (local context only, not published) pp. 1–4; mapped IDs are local.
Status values: `PASS` | `PARTIAL` | `FAIL` | `NOT_RUN`.

## Evidence baseline

| Kind | Reference |
|---|---|
| Published tip (CI green) | `bf0c76773fc609c97b3977fc8415defa9cd587d9` |
| Remote CI | [Actions run 45738641426](https://github.com/mrdouglasmorais/Payroll-Portability-Platform---Case/actions/runs/45738641426) — **SUCCESS 8/8** on `bf0c767` |
| Local follow-up session | `ai/sessions/AI-014-final-release-hardening.md` (working tree after `bf0c767`; **do not** claim CI green for unpublished commits) |
| Prior sessions | `AI-000` … `AI-013` (historical; preserved) |

| ID | Origem | Requisito | Obrigatoriedade | Spec | ADR | Implementação | Teste | Evidência | Status | Risco residual |
|---|---|---|---|---|---|---|---|---|---|---|
| RF01 | PDF p.1–2 | Opt-in / autorização com id único | Obrigatório | SPEC-001 | ADR-004 | Controllers + Central | `PortabilityApiWebTest`, dual-stack, Playwright | Local AI-013/014 + CI `bf0c767` | PASS | Sem auth de operador (lab) |
| RF02 | PDF p.2 | Agenda futura autorizada | Obrigatório | SPEC-002 | ADR-004 | `SyncAgendaService` | dual-stack, Playwright, agenda sync IT | Local + CI `bf0c767` | PASS | — |
| RF03 | PDF p.2 | Seleção 1..N + conta + elegibilidade | Obrigatório | SPEC-003 | ADR-011 | `CreatePortabilityService` | create atomic, order, concurrent | Local + CI `bf0c767` | PASS | — |
| RF04 | PDF p.2 | Async status; conta só na confirmação | Obrigatório | SPEC-004 | ADR-006/010 | Worker + Central | worker + timeout-recovery + process-kill (pós-protocolo) | Local AI-013/014 | PASS | Kill **pré**-persistência do protocolo não automatizado; mitigado por idempotência + reconciliação |
| RF05 | PDF p.2 | Trilha append-only | Obrigatório | SPEC-007 | ADR-008 | `operation_events` | create atomic, dual-stack eventos | Local + CI `bf0c767` | PASS | — |
| RF06 | PDF p.2 | Rejeição vs falha integração | Obrigatório | SPEC-008 | ADR-006 | Fault modes + REQUIRES_REVIEW | dual-stack reject, worker transient | Local + CI `bf0c767` | PASS | — |
| RNF01 | PDF p.2 | HMAC | Obrigatório | SPEC-006 | ADR-009 | Filter + gateway | Central HMAC ITs | Local + CI `bf0c767` | PASS | Lab HMAC ≠ TLS |
| RNF02 | PDF p.2 | Idempotência E2E | Obrigatório | SPEC-005 | ADR-005 | Claim+fingerprint | same-key concurrent, 409, dual-stack | Local + CI `bf0c767` | PASS | HTTP load suite still lab-only |
| RNF03 | PDF p.2 | 202 + poll + backoff | Obrigatório | SPEC-004 | ADR-010 | Worker SUBMIT/POLL | timeout-recovery PASS | Local AI-013 | PASS | — |
| RNF04 | PDF p.2 | 503/timeout/unexpected | Obrigatório | SPEC-008 | — | Fault injection | worker + Central faults | Local + prior | PASS | — |
| RNF05 | PDF p.2 | Auditoria | Obrigatório | SPEC-007 | ADR-008 | Append events | fenced append tests | Local + CI `bf0c767` | PASS | — |
| RNF06 | PDF p.2 | Manutenibilidade | Obrigatório | AGENTS | ADR-002 | Feature packages | ArchUnit | Local + CI `bf0c767` | PASS | — |
| RNF07 | PDF p.2 | Testes críticos | Obrigatório | ADR-013 | ADR-013 | JUnit/Vitest/E2E/Playwright | suite + CI 8/8 | Local AI-014 + remote `bf0c767` | PASS | CI do SHA pós-AI-014 só após push humano |
| RNF08 | PDF p.2–3 | Uso de IA | Obrigatório | ADR-015 | ADR-015 | sessions AI-000…014 | session files + human review | AI-009…014 | PASS | Narrativa de sessão ≠ prova sem comando; commits humanos |
| RNF09 | PDF p.4 | README + reproduzível | Obrigatório | — | ADR-001 | Compose + README | compose up + scripts | Local AI-014 + CI `bf0c767` | PASS | Confirm Actions on delivery SHA after each push |
| Obs | ADR-014 | Métricas + correlation | Recomendado | ADR-014 | ADR-014 | Prometheus + MDC + afterCommit | telemetry IT + curl | Local AI-014 | PASS | Métricas ≠ fonte contábil; OTel optional |
