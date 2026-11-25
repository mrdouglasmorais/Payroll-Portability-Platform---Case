# AI-004 — Portability core (M4)

- Task / spec / ADR: SPEC-003/004/005/006/008, ADR-006/009/010, OpenAPI platform + Central
- Tool and model: Cursor agent (Composer)
- Skills loaded: spec-driven-development, java-engineering, spring-boot-engineering, message-integrity, async-processing, integration-resilience, distributed-systems, idempotency-engineering, state-machine, test-engineering, ai-governance
- Constraints: HTTP to Central outside DB transactions; terminal states only after verified Central status; timeout/503 is not rejection; human owns commits

## Changes

- Platform HTTP: `/api/v1/autorizacoes`, agenda, operacoes POST/GET, eventos
- Central HMAC client (`HttpCentralGateway`) + response signature verify
- Consent/agenda proxy with local persistence of authorization and installment snapshots
- Worker lease/fence: `SUBMIT_TO_CENTRAL` → `POLL_CENTRAL` → confirm/reject finalize
- Tests: HMAC canonical, MockMvc API, worker confirm/reject/503

## Validation

```text
$ java -version
openjdk version "25.0.5.1" 2026-11-17 LTS (Corretto)

$ cd apps/portability-api && ./mvnw -B verify
Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- Validation: PASS
- Human review / decision: pending (Gate 2)
- Commit reference: pending (user-executed, Gate 3)
- Remaining risks: no dual-container E2E with live central-simulator in CI yet; response HMAC skew edge cases; UI still M5+
