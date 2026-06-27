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
E2E_PASSWORD=E2eTest!123
```

## Happy Path MVP Workflow

Runs the full Broker-to-Fleet lifecycle:

- Health check
- Admin login
- Broker register
- Fleet register
- Email OTP verification
- Phone OTP verification
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

## WebSocket Messaging Smoke Test

The bash E2E scripts validate REST messaging. Use this script after the happy
path E2E creates a conversation to validate STOMP/WebSocket publishing.

Use a broker or fleet JWT that belongs to the conversation:

```bash
TOKEN=<broker-or-fleet-jwt> \
CONVERSATION_ID=<conversation-id-from-e2e-output> \
BASE_URL=http://localhost:8080 \
node scripts/e2e/websocket_smoke.js
```

Successful output ends with:

```text
WEBSOCKET E2E PASSED
```

The script connects to `/ws`, subscribes to
`/topic/conversations/{conversationId}`, sends a message to
`/app/conversations/{conversationId}/messages`, and expects the message event
to be published back to the topic.

## Optional Variables

```bash
BASE_URL=http://localhost:8080
RUN_ID=manual-test-001
E2E_PASSWORD=E2eTest!123
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
- The WebSocket smoke test requires Node.js with global `WebSocket` support, such as Node 22+.
