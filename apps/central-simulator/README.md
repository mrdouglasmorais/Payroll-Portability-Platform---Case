# apps/central-simulator

Independent Central protocol simulator (Spring Boot 4 / Java 25).

## Local commands

```bash
./mvnw spring-boot:run
./mvnw verify
```

Health: `GET /actuator/health`  
Bootstrap status: `GET /status`

HMAC, failure injection and protocol semantics arrive in M3.
