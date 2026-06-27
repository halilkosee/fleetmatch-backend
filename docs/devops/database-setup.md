# Database Setup

Each environment must use a separate PostgreSQL database.

| Environment | Database |
| --- | --- |
| LOCAL | `fleetmatch_local` recommended, `fleetmatch` supported for existing local setup |
| DEV | `fleetmatch_dev` |
| PROD | `fleetmatch_prod` |

## Local Database

The current local profile reads `.env.local`. If `.env.local` is not present,
the fallback values are:

```text
DB_HOST=localhost
DB_PORT=5432
DB_NAME=fleetmatch
DB_USERNAME=halil
DB_PASSWORD=
```

For a fully isolated local database, set `.env.local` to `fleetmatch_local`
and create the role/database below.

Using Docker:

```bash
docker compose --env-file .env.local -f docker-compose.dev.yml up -d postgres redis
```

Using local PostgreSQL:

```bash
POSTGRES_USER=postgres scripts/setup-local-db.sh
```

Equivalent SQL:

```sql
CREATE USER fleetmatch_local WITH PASSWORD 'fleetmatch_local';
CREATE DATABASE fleetmatch_local OWNER fleetmatch_local;
GRANT ALL PRIVILEGES ON DATABASE fleetmatch_local TO fleetmatch_local;
GRANT USAGE, CREATE ON SCHEMA public TO fleetmatch_local;
ALTER SCHEMA public OWNER TO fleetmatch_local;
```

## DEV Database

DEV should use `fleetmatch_dev` only. Developers and frontend/mobile clients should access the backend through:

```text
https://api-dev.easyfleetmatch.com
```

## PROD Database

PROD should use `fleetmatch_prod` only. Do not connect local or DEV services to the production database.

## Migrations

Flyway runs automatically on application startup:

```yaml
spring.flyway.enabled: true
spring.flyway.locations: classpath:db/migration
```

Keep migrations append-only. Do not edit already-applied migration files.
