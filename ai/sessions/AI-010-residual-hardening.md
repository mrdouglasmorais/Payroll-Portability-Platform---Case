# AI-010 — Residual hardening (V8 policy, agenda race, Central lock)

## Prompt (human)
Independent residual review: V8 migration financial hazard, SyncAgenda race on effective/status, Central confirm race, raw-body idempotency docs, reconciliation runbook depth, P2 lease/Retry-After/typed errors.

## Changes
- Left `V8` checksum immutable; added `V9__lab_reset_policy.sql` (no-op marker) + `scripts/lab-reset-demo-parcels.sh`
- Agenda sync: `updateOpenAgendaAttributes` (`WHERE status='OPEN'`, no effective/status writes)
- Central `status()`: `FOR UPDATE` + conditional `PENDENTE` transitions
- Typed `IntegrationFailureCode` on `CentralException` at HMAC/gateway origin
- `Retry-After` delta-seconds + HTTP-date; docs for idempotency vs HMAC and lease semantics
- Runbook distinguishes available vs proposed vs forbidden ops

## Validation (actual)
```
$ ./apps/portability-api/mvnw -B verify   # Tests run: 30 — BUILD SUCCESS
$ ./apps/central-simulator/mvnw -B verify # Tests run: 9 — BUILD SUCCESS
$ cd apps/web && pnpm test                # 6 passed
```

## NOT_RUN
- Executable admin reconciliation CLI (still poll/fence path only)
- Independent reproduction of agenda∩portability race (fixed by static analysis + conditional UPDATE)

## Later
axe/keyboard covered in AI-013 Playwright suite. Human review performed (candidate).
