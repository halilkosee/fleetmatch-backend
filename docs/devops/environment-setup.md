# EasyFleetMatch Backend Environment Setup

EasyFleetMatch backend supports three runtime environments:

- `LOCAL`: developer machine
- `DEV`: shared development server, public API at `https://api-dev.easyfleetmatch.com`
- `PROD`: production server, public API at `https://api.easyfleetmatch.com`

Use Spring profiles:

```bash
SPRING_PROFILES_ACTIVE=local
SPRING_PROFILES_ACTIVE=dev
SPRING_PROFILES_ACTIVE=prod
```

## Required Environment Variables

```bash
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=local|dev|prod
FLEETMATCH_ENVIRONMENT=LOCAL|DEV|PROD

DB_HOST=localhost
DB_PORT=5432
DB_NAME=fleetmatch_local
DB_USERNAME=fleetmatch_local
DB_PASSWORD=change-me

JWT_SECRET=minimum-64-character-secret
JWT_EXPIRATION_MS=86400000

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

API_BASE_URL=https://api-dev.easyfleetmatch.com
FRONTEND_BASE_URL=https://dev.easyfleetmatch.com
CORS_ALLOWED_ORIGINS=https://dev.easyfleetmatch.com,https://app-dev.easyfleetmatch.com

MAIL_HOST=
MAIL_PORT=587
MAIL_USERNAME=
MAIL_PASSWORD=
MAIL_FROM=no-reply@easyfleetmatch.com
```

## Environment Files

The repository includes environment templates:

- `.env.example`
- `.env.local`
- `.env.dev`
- `.env.prod`

Replace placeholder passwords and secrets on real servers. Do not commit real production secrets.
