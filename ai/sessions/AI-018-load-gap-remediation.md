# AI-018 — Load-test gap remediation

**Date:** 2026-11-26  
**Author:** Douglas Morais  
**Skills:** ai-governance, observability (ECS), java-engineering  

## Prior commits (ready work)

- `7d87fa0` feat: ECS logs, OTel, Grafana Traceability lab  
- `fc0823d` docs: dual-DB model + docs-only clean-seed  
- `8505961` fix: k6 M6 `API_BASE` via host.docker.internal  

## Gaps closed this session

| Gap | Change |
| --- | --- |
| P0 dual-DB desync | `RequestAuthorizationService` always re-POSTs Central authorize (idempotent heal); `scripts/lab-sync-central-authorizations.sh` |
| P1 agenda 403 silent | `central.agenda.forbidden` on API + Central; `central.call.failed` on `CentralException` handler |
| P2 M6 single wave | Documented in `tests/performance/README.md` + load-test runbook |
| P3 WARN noise | Filter note in `observability.md` (level unchanged) |
| ECS duplicate keys | Stop `addKeyValue(correlationId/attempt)` when already in MDC (broke appender) |
| Central structured events | `central.authorization.accepted`, `central.agenda.forbidden` |

## Evidence

```text
# Unit
./mvnw -Dtest=RequestAuthorizationServiceTest,StructuredConfirmLogTest,SensitiveDataTest test
# → PASS

# Heal
DELETE FROM authorizations WHERE beneficiary_id='ben-001';
GET agenda → 403 + event central.agenda.forbidden (API + Central)
POST /autorizacoes → Central row restored
GET agenda → 200
```

## Unresolved

- Host Prometheus (`*_seconds`) vs LGTM (`*_milliseconds`) remains dual by design.  
- Sustained multi-wave confirm load still needs new parcels or volume recreate (no financial reopen).  
