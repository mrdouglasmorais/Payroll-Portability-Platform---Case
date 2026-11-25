# apps/portability-api

Spring Boot 4 / Java 25 service for the payroll portability platform.

## Local commands

```bash
./mvnw spring-boot:run
./mvnw verify
```

Health: `GET /actuator/health`  
Bootstrap status: `GET /api/v1/status`

Domain features arrive in later milestones. M1 only proves boot, Flyway, JDBC and health.
