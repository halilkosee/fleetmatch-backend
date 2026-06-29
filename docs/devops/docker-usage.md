# Docker Usage

## Build Backend Image

```bash
docker build -t easyfleetmatch-backend:local .
```

The Docker build context excludes local build output, IDE files, logs, and real environment files.

## Run DEV Stack

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml up -d --build
```

DEV services:

- backend: `http://localhost:8080`
- postgres: host port `5433`, container port `5432`
- redis: host port `6380`, container port `6379`

DEV ports are bound to `127.0.0.1`; public DEV access should go through Nginx.

## Run PROD Stack

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

PROD compose keeps PostgreSQL and Redis internal to Docker. Only backend port `8080` is published. A reverse proxy should route:

```text
https://api.easyfleetmatch.com -> backend:8080
https://api-dev.easyfleetmatch.com -> backend:8080
```

The Docker image and compose files include health checks for backend, PostgreSQL, and Redis.

## Nginx Configs

Production-ready reverse proxy examples are available in:

```text
deploy/nginx/dev.conf
deploy/nginx/prod.conf
```

Validate and reload Nginx after copying the matching file into the server's Nginx sites directory.

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
