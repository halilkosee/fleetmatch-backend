#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env.dev}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.dev.yml}"
SEED_FILE="${SEED_FILE:-scripts/dev-seed.sql}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing environment file: $ENV_FILE" >&2
  exit 1
fi

if [[ ! -f "$SEED_FILE" ]]; then
  echo "Missing seed file: $SEED_FILE" >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

DB_NAME="${DB_NAME:-fleetmatch_dev}"
DB_USERNAME="${DB_USERNAME:-fleetmatch_dev}"

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T postgres \
  psql -v ON_ERROR_STOP=1 -U "$DB_USERNAME" -d "$DB_NAME" < "$SEED_FILE"

cat <<'EOF'
DEV seed completed.

Demo password for all seeded users:
  DevAdmin!123

Seeded users:
  admin@easyfleetmatch.dev
  broker.owner@easyfleetmatch.dev
  fleet.owner@easyfleetmatch.dev
  review.broker@easyfleetmatch.dev
  review.fleet@easyfleetmatch.dev
EOF
