# Release readiness report — AI-012

Engineering status of the repository after the AI-012 hardening pass. Quality should be judged from code, tests and recorded command evidence — not from narrative claims in this document.

## 1. Executive summary

| Field | Value |
|---|---|
| Base SHA analyzed | `7e71031` |
| Delivery SHA (post AI-012 commit) | see `git log -1` on `main` |
| Milestone | M9 + AI-010 residual + AI-011 pt-BR + AI-012 readiness |
| Local gates | Critical Maven / web / Compose / E2E / Playwright PASS (see §5) |
| Remote CI on delivery SHA | Verify on GitHub Actions after push |
| Main changes | Deterministic multi-parcel lock order; concurrent same-key + agenda sync regressions; README/run docs; Playwright CI gate; compliance matrix |

## 2. Compliance matrix

See [compliance-matrix.md](compliance-matrix.md). RF01–RF06 and RNF01–RNF07 marked **PASS** with local evidence in AI-012. RNF08/RNF09 remain **PARTIAL** where human review or remote CI evidence is still required.

## 3. Findings

| ID | Priority | File | Problem | Impact | Correction | Status |
|---|---|---|---|---|---|---|
| E | P0 | CreatePortabilityService / CentralProtocolService | Non-deterministic multi-parcel order | Deadlock risk | Sorted reserve + TreeSet | Closed |
| C-test | P1 | JdbcInstallmentRepository | OPEN-only sync untested | Silent overwrite risk | Agenda sync IT | Closed |
| Idemp | P1 | CreatePortabilityServiceTest | Concurrent same-key thin | Duplicate op risk | 4-thread same-key test | Closed |
| README | P1 | README.md | Incomplete run / demo / library sections | Harder clean checkout | Rewrite | Closed |
| CI-UI | P1 | ci.yml | No Playwright/web health | UI regressions | Compose+Playwright | Closed in workflow; confirm Actions on delivery SHA |
| V8 | P1 | Central V8 | Demo reopen on migrate | Lab invariant hazard | V9 + script + docs (no checksum rewrite) | Accepted residual |
| A11y | P2 | web | No axe CI | a11y gaps | Deferred | Open |
| Kill | P2 | — | Process-kill chaos | Recovery proof thin | Deferred | Open |

## 4. Files changed (this session)

| File | Why |
|---|---|
| `CreatePortabilityService.java` | Sort installments before reserve |
| `JdbcInstallmentRepository.java` | `ORDER BY` on multi-get |
| `CentralProtocolService.java` | `TreeSet` parcel order |
| `CreatePortabilityServiceTest.java` | Order + concurrent same-key |
| `JdbcInstallmentRepositoryAgendaSyncTest.java` | RESERVED/PORTED immunity |
| `ci.yml` | Web health + Playwright + lab-reset |
| `operator-flow.spec.ts` | Stable locators |
| `README.md`, `acceptance.md`, `compliance-matrix.md`, `ai/*` | Run docs + evidence |
| `Consent.java`, `HttpCentralGateway.java` | PT-BR messages (AI-011) |

## 5. Tests

| Command | Result |
|---|---|
| `apps/portability-api ./mvnw -B clean verify` | **34** run, 0 fail, 0 skip — SUCCESS |
| `apps/central-simulator ./mvnw -B clean verify` | **9** run, 0 fail — SUCCESS |
| `apps/web pnpm test && build` | **9** tests / 5 files — PASS |
| Redocly lint | Valid (2 localhost warnings) |
| `dual-stack.sh` | `DUAL_STACK_E2E_PASS` |
| `timeout-recovery.sh` | `TIMEOUT_RECOVERY_E2E_PASS` |
| Playwright | **1 passed** (after lab-reset; not parallel with API restart) |
| GitHub Actions @ delivery SHA | Confirm on the pushed commit |

## 6. Security

- HMAC bidirectional + replay/nonce tests PASS locally.
- No real secrets committed (lab `.env.example`).
- Operator auth absent (documented lab limit).
- Gitleaks/CodeQL/Trivy present in CI.

## 7. Architecture

Modular monolith + independent Central + dual Postgres + leased jobs + HMAC + local TX / eventual confirm. Finding E closed. V8 residual documented. No unjustified brokers/CQRS.

## 8. Frontend

Operator wizard in pt-BR; idempotency key reuse; poll stops on terminal status; Playwright happy path PASS locally. axe/keyboard automation still open (P2).

## 9. CI/CD

Jobs: Maven×2, frontend, OpenAPI, compose-smoke (+web+Playwright), Gitleaks, CodeQL, Trivy. No `continue-on-error`. Observe the Actions run for the delivery SHA.

## 10. Residual risks

- **Open before treating the tree as fully gated:** Actions success on the delivery SHA; human review notes on financial sessions where still pending.
- **Acceptable lab limits:** no operator SSO; synthetic data; V8 history on clean migrate; Central EN messages.
- **Future work:** axe CI, process-kill recovery proof, reconciliation CLI, auth, retention.

## 11. Git

Working tree and `git status` / `git log -1` are the source of truth for what is published. Prefer small, reviewable commits that map to the findings above.
