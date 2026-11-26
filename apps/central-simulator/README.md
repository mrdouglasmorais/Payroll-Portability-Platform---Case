# apps/central-simulator

Simulador independente do protocolo da Central (Spring Boot 4 / Java 25).

## Comandos locais

```bash
./mvnw spring-boot:run
./mvnw verify
```

Health: `GET /actuator/health`  
Status: `GET /status`

## Injeção de falhas

| Precedência | Mecanismo |
|---|---|
| 1 | Header `X-Sim-Fault` (`REJECT`, `TIMEOUT`, `UNAVAILABLE`, `CONFIRM`) |
| 2 | Sufixo do `beneficiarioId` (`-reject`, `-timeout`, `-503`) |
| 3 | Probabilístico opcional: `CENTRAL_FAULT_RATE` + `CENTRAL_FAULT_SEED` |

CI e dual-stack usam taxa `0` (determinístico). Para demos “ocasionais”: `CENTRAL_FAULT_RATE=0.2`.
