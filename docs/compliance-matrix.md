# Compliance matrix (release readiness)

Source of truth: original challenge PDF (local context only, not published) pp. 1–4; mapped IDs are local.
Status values: `PASS` | `PARTIAL` | `FAIL` | `NOT_RUN`.
Evidence session: `ai/sessions/AI-012-release-readiness.md` (HEAD base `7e71031` + uncommitted AI-012 changes).

| ID | Origem | Requisito | Obrigatoriedade | Spec | ADR | Implementação | Teste | Evidência | Status | Risco residual |
|---|---|---|---|---|---|---|---|---|---|---|
| RF01 | PDF p.1–2 | Opt-in / autorização com id único | Obrigatório | SPEC-001 | ADR-004 | Controllers + Central | `PortabilityApiWebTest`, dual-stack, Playwright | AI-012 Maven+Compose+UI | PASS | Sem auth de operador (lab) |
| RF02 | PDF p.2 | Agenda futura autorizada | Obrigatório | SPEC-002 | ADR-004 | `SyncAgendaService` | dual-stack, Playwright, agenda sync IT | AI-012 | PASS | — |
| RF03 | PDF p.2 | Seleção 1..N + conta + elegibilidade | Obrigatório | SPEC-003 | ADR-011 | `CreatePortabilityService` | create atomic, order, concurrent | AI-012 34 tests | PASS | — |
| RF04 | PDF p.2 | Async status; conta só na confirmação | Obrigatório | SPEC-004 | ADR-006/010 | Worker + Central | worker + timeout-recovery | AI-012 | PASS | Process-kill real NOT_RUN |
| RF05 | PDF p.2 | Trilha append-only | Obrigatório | SPEC-007 | ADR-008 | `operation_events` | create atomic, dual-stack eventos | AI-012 | PASS | — |
| RF06 | PDF p.2 | Rejeição vs falha integração | Obrigatório | SPEC-008 | ADR-006 | Fault modes + REQUIRES_REVIEW | dual-stack reject, worker transient | AI-012 | PASS | — |
| RNF01 | PDF p.2 | HMAC | Obrigatório | SPEC-006 | ADR-009 | Filter + gateway | Central HMAC ITs | AI-012 central verify | PASS | Lab HMAC ≠ TLS |
| RNF02 | PDF p.2 | Idempotência E2E | Obrigatório | SPEC-005 | ADR-005 | Claim+fingerprint | same-key concurrent, 409, dual-stack | AI-012 | PASS | HTTP load suite still lab-only |
| RNF03 | PDF p.2 | 202 + poll + backoff | Obrigatório | SPEC-004 | ADR-010 | Worker SUBMIT/POLL | timeout-recovery PASS | AI-012 | PASS | — |
| RNF04 | PDF p.2 | 503/timeout/unexpected | Obrigatório | SPEC-008 | — | Fault injection | worker + Central faults | AI-012 + prior | PASS | — |
| RNF05 | PDF p.2 | Auditoria | Obrigatório | SPEC-007 | ADR-008 | Append events | fenced append tests | AI-012 | PASS | — |
| RNF06 | PDF p.2 | Manutenibilidade | Obrigatório | AGENTS | ADR-002 | Feature packages | ArchUnit | AI-012 | PASS | — |
| RNF07 | PDF p.2 | Testes críticos | Obrigatório | ADR-013 | ADR-013 | JUnit/Vitest/E2E/Playwright | AI-012 suite | AI-012 | PASS | CI remota do SHA final NOT_RUN até push |
| RNF08 | PDF p.2–3 | Uso de IA | Obrigatório | ADR-015 | ADR-015 | sessions AI-000…012 | session files | AI-012 | PARTIAL | Human review pending |
| RNF09 | PDF p.4 | README + reproduzível | Obrigatório | — | ADR-001 | Compose + README | compose up + scripts | AI-012 | PARTIAL | origin/main ainda 1+ commits atrás até push |
