# infra

Local execution is driven from the repository root:

```bash
cp .env.example .env
docker compose up --build
```

| Service | URL |
| --- | --- |
| Web | http://localhost:5173 |
| Portability API | http://localhost:8080/actuator/health |
| Central simulator | http://localhost:8081/actuator/health |
| Platform PostgreSQL | localhost:5432 |
| Central PostgreSQL | localhost:5433 |

Compose file: [`../docker-compose.yml`](../docker-compose.yml).
