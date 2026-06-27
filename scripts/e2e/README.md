# Backend E2E Scripts

These scripts run API-level smoke tests against a running EasyFleetMatch backend.

They are intended for LOCAL and DEV validation, not for production data.

## Requirements

- Backend is running.
- `curl` is installed.
- `jq` is installed.
- An active admin user exists.

Example admin values used locally:

```bash
ADMIN_EMAIL=admin@fleetmatch.com
ADMIN_PASSWORD=123456
```

## Happy Path MVP Workflow

Runs the full Broker-to-Fleet lifecycle:

- Health check
- Admin login
- Broker register
- Fleet register
- User approval
- Company approval
- PRO subscription assignment
- Broker load creation
- Fleet offer submission
- Broker offer selection
- Conversation creation
- Messaging
- Fleet assignment confirmation
- Load start
- Load delivery

```bash
ADMIN_EMAIL=admin@fleetmatch.com \
ADMIN_PASSWORD=123456 \
BASE_URL=http://localhost:8080 \
bash scripts/e2e/backend_e2e.sh
```

Successful output ends with:

```text
E2E PASSED
Final load status: DELIVERED
```

## Negative And Security Workflow

Runs checks for authorization, subscription, messaging isolation, soft delete,
and decline/archive behavior.

```bash
ADMIN_EMAIL=admin@fleetmatch.com \
ADMIN_PASSWORD=123456 \
BASE_URL=http://localhost:8080 \
bash scripts/e2e/backend_negative_e2e.sh
```

Successful output ends with:

```text
NEGATIVE E2E PASSED
```

## Optional Variables

```bash
BASE_URL=http://localhost:8080
RUN_ID=manual-test-001
E2E_PASSWORD=123456
```

`RUN_ID` is used in generated emails and reference numbers. If omitted, the
current Unix timestamp is used.

## DEV Environment

After `api-dev.easyfleetmatch.com` is live, run the same scripts against DEV:

```bash
ADMIN_EMAIL=<dev-admin-email> \
ADMIN_PASSWORD=<dev-admin-password> \
BASE_URL=https://api-dev.easyfleetmatch.com \
bash scripts/e2e/backend_e2e.sh

ADMIN_EMAIL=<dev-admin-email> \
ADMIN_PASSWORD=<dev-admin-password> \
BASE_URL=https://api-dev.easyfleetmatch.com \
bash scripts/e2e/backend_negative_e2e.sh
```

## Notes

- The scripts create test companies, users, loads, offers, conversations, and messages.
- Use a non-production database.
- The scripts assign the `PRO` plan where needed to exercise offer and load workflows.
- The negative script intentionally expects HTTP `400` or `403` responses for blocked actions.
