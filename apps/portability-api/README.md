# apps/portability-api

API da plataforma de portabilidade (Spring Boot 4 / Java 25).

## Comandos locais

```bash
./mvnw spring-boot:run
./mvnw verify
```

Ver também a seção **Rodando sem Docker** no README da raiz.

Health: `GET /actuator/health`  
Status: `GET /api/v1/status`

## M2 domain & persistence

Flyway `V2__domain_persistence.sql` creates beneficiaries, consents, installment snapshots, operations, active reservations (partial unique index), append-only events, idempotency keys and processing jobs.

Package layout (hexagonal by feature):

- `consent` / `schedule` / `portability` / `shared`
- Domain invariants enforced in aggregates (`Money`, `PortabilityOperation`, `Consent`, `InstallmentSnapshot`)
- JDBC adapters implement application ports; `CreatePortabilityService` creates operation + reservation + event + job atomically

```bash
./mvnw verify
```

HTTP opt-in/agenda/operation APIs and Central calls arrive in M3/M4.
