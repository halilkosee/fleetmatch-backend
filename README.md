# fleetmatch-backend
FleetMatch is a modern broker-carrier load board platform with load posting, offer management, booking workflow, and fleet operations support.

## DevOps

Backend environment setup is documented in `docs/devops/`.

- Local API: `http://localhost:8080`
- DEV API: `https://api-dev.easyfleetmatch.com`
- PROD API: `https://api.easyfleetmatch.com`

Quick local run:

```bash
docker compose --env-file .env.local -f docker-compose.dev.yml up -d postgres redis
export $(grep -v '^#' .env.local | xargs)
./mvnw spring-boot:run
```

Quick DEV stack:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d --build
```
