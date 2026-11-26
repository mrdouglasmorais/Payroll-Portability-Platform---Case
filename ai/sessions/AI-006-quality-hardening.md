# AI-006 — Quality hardening / dual-stack E2E / load smoke (M6)

- Task / spec / ADR: acceptance gates, SPEC-008/009, ADR-013, Miro frames 09/13/18
- Tool and model: Cursor agent (Composer)
- Skills loaded: end-to-end-testing, performance-engineering, test-engineering, documentation, ai-governance
- Constraints: do not invent PASS; record actual commands; k6 thresholds are lab smoke gates only

## Changes

- `tests/e2e/dual-stack.sh` — API happy path + reject + idempotency 409
- Playwright operator flow (`tests/e2e/specs/operator-flow.spec.ts`) against Compose web
- `tests/performance/k6-m6-portability.js` — platform authorize/agenda/create smoke
- Central Flyway V3–V5 seeds (reject/503, load parcels, UI parcels)
- Compose env: `CENTRAL_HMAC_KEY_ID`, `CENTRAL_CONFIRM_DELAY=PT1S`
- `docs/acceptance.md` mapped to evidence; one gap left (timeout recovery restart harness)

## Validation (actual)

```text
$ docker compose up --build -d
# services healthy

$ ./tests/e2e/dual-stack.sh
DUAL_STACK_E2E_PASS
# confirm + reject terminal statuses observed

$ docker run --rm -i --network host -e API_BASE=http://localhost:8080 \
    grafana/k6:0.54.0 run - < tests/performance/k6-m6-portability.js
checks.........................: 100.00%
errors.........................: 0.00%
http_req_failed................: 0.00%
smoke ✓ 3 VUs 45s

$ cd tests/e2e && WEB_BASE=http://localhost:5173 pnpm test
1 passed (operator flow → CONFIRMADA)
```

- Validation: PASS (with documented gap on timeout-restart E2E)
- Human review / decision: pending
- Commit reference: pending (user-executed)
- Remaining risks: timeout/lost-response restart path not Compose-automated; k6 not a production SLO; Playwright depends on dedicated `ben-ui` seeds
