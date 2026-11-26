# Requirements traceability

Source: original technical challenge, pp. 1–4. IDs assigned locally. Architectural decisions guided by the Miro [system design](architecture/system-design.md) board. Formal status rows: [compliance-matrix.md](compliance-matrix.md).

| Requirement | Specification | Verification |
|---|---|---|
| RF1 | [SPEC-001-consent](specs/SPEC-001-consent.md) | Given no consent, agenda access fails; after accepted opt-in, matching agreement is accessible. |
| RF2 | [SPEC-002-schedule](specs/SPEC-002-schedule.md) | Unauthorized agreement excluded; open future installments shown; failures visible. |
| RF3 | [SPEC-003-portability](specs/SPEC-003-portability.md) | Concurrent competing operations cannot reserve same installment; invalid selection rejected. |
| RF4 | [SPEC-004-async](specs/SPEC-004-async.md) | Lost response recovered with stable key; restart resumes; rejection preserves prior account. |
| RNF2 | [SPEC-005-idempotency](specs/SPEC-005-idempotency.md) | Concurrent equal requests produce one operation and one protocol; different keys cannot double-port same installment. |
| RNF1 | [SPEC-006-integrity](specs/SPEC-006-integrity.md) | Reject altered body, invalid signature, expired timestamp and replay; fresh retry nonce preserves key. |
| RF5/RNF5 | [SPEC-007-audit](specs/SPEC-007-audit.md) | State and event commit together; prior events remain unchanged. |
| RF6/RNF3/RNF4 | [SPEC-008-failure-recovery](specs/SPEC-008-failure-recovery.md) | Timeout, 503, malformed response, invalid signature and worker crash are distinguishable; reservations stay held for ambiguous outcome. |
| RF1–RF6 | [SPEC-009-web](specs/SPEC-009-web.md) | Accessible end-to-end success/rejection/failure flows; no duplicate on repeated submit; polling stops terminal. |
| RNF6 maintainability / RNF7 tests | AGENTS.md / acceptance.md | Architecture and critical behavior tests |
| RNF8 AI usage / delivery | ai/README.md / README.md | Actual session evidence and clean checkout |
