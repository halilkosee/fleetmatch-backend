#!/usr/bin/env bash
set -euo pipefail

DB_NAME="${DB_NAME:-fleetmatch_local}"
DB_USERNAME="${DB_USERNAME:-fleetmatch_local}"
DB_PASSWORD="${DB_PASSWORD:-fleetmatch_local}"
POSTGRES_DB="${POSTGRES_DB:-postgres}"
POSTGRES_USER="${POSTGRES_USER:-postgres}"

psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" <<SQL
DO
\$\$
BEGIN
   IF NOT EXISTS (
      SELECT FROM pg_catalog.pg_roles
      WHERE rolname = '${DB_USERNAME}'
   ) THEN
      CREATE ROLE ${DB_USERNAME} LOGIN PASSWORD '${DB_PASSWORD}';
   ELSE
      ALTER ROLE ${DB_USERNAME} WITH LOGIN PASSWORD '${DB_PASSWORD}';
   END IF;
END
\$\$;

SELECT 'CREATE DATABASE ${DB_NAME} OWNER ${DB_USERNAME}'
WHERE NOT EXISTS (
   SELECT FROM pg_database WHERE datname = '${DB_NAME}'
)\gexec

GRANT ALL PRIVILEGES ON DATABASE ${DB_NAME} TO ${DB_USERNAME};
SQL

psql -U "$POSTGRES_USER" -d "$DB_NAME" <<SQL
GRANT USAGE, CREATE ON SCHEMA public TO ${DB_USERNAME};
ALTER SCHEMA public OWNER TO ${DB_USERNAME};
SQL

echo "Local database is ready: ${DB_NAME} / ${DB_USERNAME}"
