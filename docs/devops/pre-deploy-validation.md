# Pre-Deploy Validation

Use this checklist before moving backend changes to DEV or PROD.

## 1. Clean Local Database

This deletes local test data. Use only on a non-production database.

For the current local setup:

```bash
psql -h localhost -p 5432 -U halil -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'fleetmatch';"
psql -h localhost -p 5432 -U halil -d postgres -c "DROP DATABASE IF EXISTS fleetmatch;"
psql -h localhost -p 5432 -U halil -d postgres -c "CREATE DATABASE fleetmatch OWNER halil;"
```

For the isolated local setup:

```bash
POSTGRES_USER=postgres scripts/setup-local-db.sh
```

## 2. Start Backend

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Confirm:

```bash
curl http://localhost:8080/api/health
```

## 3. Run Full E2E

```bash
ADMIN_EMAIL=admin@fleetmatch.com \
ADMIN_PASSWORD=123456 \
BASE_URL=http://localhost:8080 \
bash scripts/e2e/backend_e2e.sh
```

Expected final line:

```text
E2E PASSED
```

## 4. Run Negative E2E

```bash
ADMIN_EMAIL=admin@fleetmatch.com \
ADMIN_PASSWORD=123456 \
BASE_URL=http://localhost:8080 \
bash scripts/e2e/backend_negative_e2e.sh
```

Expected final line:

```text
NEGATIVE E2E PASSED
```

## 5. Run WebSocket Smoke Test

After the happy path E2E creates a conversation, login as the broker or fleet
user and use the token with the conversation id from the E2E output:

```bash
TOKEN=<broker-or-fleet-jwt> \
CONVERSATION_ID=<conversation-id-from-e2e-output> \
BASE_URL=http://localhost:8080 \
node scripts/e2e/websocket_smoke.js
```

Expected final line:

```text
WEBSOCKET E2E PASSED
```

## 6. DEV Server Readiness

Before opening `api-dev.easyfleetmatch.com` to frontend and mobile developers:

- `.env.dev` exists on the DEV server.
- `SPRING_PROFILES_ACTIVE=dev`.
- `DB_NAME=fleetmatch_dev`.
- DEV PostgreSQL and Redis use named Docker volumes.
- Nginx uses `deploy/nginx/dev.conf`.
- DNS points `api-dev.easyfleetmatch.com` to the DEV server.
- TLS certificate is installed.
- Backend health check returns HTTP 200.
- Full E2E and negative E2E pass against `https://api-dev.easyfleetmatch.com`.

## 7. PROD Server Readiness

Before production:

- `.env.prod` exists only on the PROD server.
- `SPRING_PROFILES_ACTIVE=prod`.
- `DB_NAME=fleetmatch_prod`.
- PROD secrets are different from DEV and LOCAL.
- Nginx uses `deploy/nginx/prod.conf`.
- DNS points `api.easyfleetmatch.com` to the PROD server.
- TLS certificate is installed.
- A database backup strategy exists.
- Deployment rollback steps are documented.
