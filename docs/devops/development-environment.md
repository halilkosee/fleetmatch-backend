# Development Environment

This runbook describes a shared DEV backend environment for EasyFleetMatch.

## Goal

Use one repeatable DEV stack for frontend integration, QA, and admin workflow testing:

- Backend on `api-dev.easyfleetmatch.com`
- PostgreSQL in Docker
- Redis in Docker
- Nginx reverse proxy
- Docker Compose lifecycle
- Separate DEV secrets and database

## Server Requirements

- Ubuntu 22.04+ or similar Linux host
- Docker and Docker Compose plugin
- Nginx
- DNS record for `api-dev.easyfleetmatch.com`
- TLS certificate, recommended via Certbot

## Environment File

Create `.env.dev` on the DEV server. Do not reuse local or production secrets.

Minimum required values:

```bash
SPRING_PROFILES_ACTIVE=dev
FLEETMATCH_ENVIRONMENT=DEV
SERVER_PORT=8080

DB_HOST=postgres
DB_PORT=5432
DB_NAME=fleetmatch_dev
DB_USERNAME=fleetmatch_dev
DB_PASSWORD=replace-with-dev-db-password

JWT_SECRET=replace-with-dev-jwt-secret-at-least-64-characters
JWT_EXPIRATION_MS=86400000

REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=
RATE_LIMIT_STORE=redis

API_BASE_URL=https://api-dev.easyfleetmatch.com
FRONTEND_BASE_URL=https://dev.easyfleetmatch.com
CORS_ALLOWED_ORIGINS=https://dev.easyfleetmatch.com,https://app-dev.easyfleetmatch.com,http://localhost:5173,http://127.0.0.1:5173

MAIL_PROVIDER=log
SMS_PROVIDER=log
PUSH_PROVIDER=log
DOCUMENT_STORAGE_PROVIDER=local
DOCUMENT_STORAGE_LOCAL_PATH=/app/data/company-documents
```

For real email/SMS/push testing, replace `MAIL_PROVIDER=log`, `SMS_PROVIDER=log`, and `PUSH_PROVIDER=log` with the configured SMTP/Twilio/FCM values.

## Start DEV

From the repository root:

```bash
scripts/dev-stack.sh up
```

The DEV compose file binds PostgreSQL, Redis, and backend to `127.0.0.1`. Public traffic should enter through Nginx only.

## Nginx

Copy the DEV config:

```bash
sudo cp deploy/nginx/dev.conf /etc/nginx/sites-available/fleetmatch-dev-api
sudo ln -sf /etc/nginx/sites-available/fleetmatch-dev-api /etc/nginx/sites-enabled/fleetmatch-dev-api
sudo nginx -t
sudo systemctl reload nginx
```

Then configure TLS with Certbot or your chosen certificate manager.

## Health Checks

Local server checks:

```bash
scripts/dev-stack.sh status
scripts/dev-stack.sh smoke
```

Public checks:

```bash
curl -fsS https://api-dev.easyfleetmatch.com/api/health
curl -fsS https://api-dev.easyfleetmatch.com/actuator/health/readiness
```

## Logs

```bash
scripts/dev-stack.sh logs backend
scripts/dev-stack.sh logs postgres
scripts/dev-stack.sh logs redis
```

## Update DEV

```bash
git pull
scripts/dev-stack.sh restart
scripts/dev-stack.sh smoke
```

## Automatic Deploy From GitHub

The repository includes `.github/workflows/deploy-dev.yml`.

When `develop` is pushed, GitHub Actions connects to the DEV server over SSH and runs:

```bash
scripts/deploy-dev.sh
```

Required GitHub repository secrets:

```text
DEV_SSH_HOST
DEV_SSH_USER
DEV_SSH_PRIVATE_KEY
DEV_SSH_PORT
DEV_APP_DIR
```

Recommended server path:

```text
/opt/easyfleetmatch/backend
```

`DEV_APP_DIR` is optional if the recommended path is used. `DEV_SSH_PORT` is optional when SSH runs on port `22`.

The DEV server must already have:

- repository cloned into `DEV_APP_DIR`
- `.env.dev` created on the server
- Docker and Docker Compose installed
- Nginx configured for `api-dev.easyfleetmatch.com`
- the GitHub deploy key allowed to pull the repository

Manual deploy on the server:

```bash
APP_DIR=/opt/easyfleetmatch/backend BRANCH=develop scripts/deploy-dev.sh
```

## Reset DEV Data

This deletes DEV database and Redis data:

```bash
docker compose --env-file .env.dev -f docker-compose.dev.yml down -v
scripts/dev-stack.sh up
```

Or run the guarded reset script:

```bash
scripts/dev-reset.sh --yes
```

To reset DEV and load repeatable demo data:

```bash
scripts/dev-reset.sh --yes --seed
```

The seed script can also be run by itself after the stack is already healthy:

```bash
scripts/dev-seed.sh
```

Seeded DEV accounts use the same password:

```text
DevAdmin!123
```

Seeded users:

```text
admin@easyfleetmatch.dev
broker.owner@easyfleetmatch.dev
fleet.owner@easyfleetmatch.dev
review.broker@easyfleetmatch.dev
review.fleet@easyfleetmatch.dev
```

## Frontend Integration

Frontend should use:

```bash
VITE_API_BASE_URL=https://api-dev.easyfleetmatch.com
```

CORS must include the frontend DEV domain.
