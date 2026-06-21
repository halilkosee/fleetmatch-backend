# Docker Usage

## Build Backend Image

```bash
docker build -t easyfleetmatch-backend:local .
```

## Run DEV Stack

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d --build
```

DEV services:

- backend: `http://localhost:8080`
- postgres: host port `5433`, container port `5432`
- redis: host port `6380`, container port `6379`

## Run PROD Stack

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

PROD compose keeps PostgreSQL and Redis internal to Docker. Only backend port `8080` is published. A reverse proxy should route:

```text
https://api.easyfleetmatch.com -> backend:8080
https://api-dev.easyfleetmatch.com -> backend:8080
```

## Stop Stack

```bash
docker compose -f docker-compose.dev.yml down
docker compose -f docker-compose.prod.yml down
```

## Remove Volumes

This deletes database data.

```bash
docker compose -f docker-compose.dev.yml down -v
docker compose -f docker-compose.prod.yml down -v
```
