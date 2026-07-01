#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env.dev}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.dev.yml}"
WITH_SEED=false
CONFIRMED=false

for arg in "$@"; do
  case "$arg" in
    --seed)
      WITH_SEED=true
      ;;
    --yes)
      CONFIRMED=true
      ;;
    *)
      echo "Usage: $0 --yes [--seed]" >&2
      exit 1
      ;;
  esac
done

if [[ "$CONFIRMED" != "true" ]]; then
  echo "This deletes all DEV Postgres and Redis data. Re-run with --yes to continue." >&2
  exit 1
fi

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" down -v
scripts/dev-stack.sh up

if [[ "$WITH_SEED" == "true" ]]; then
  scripts/dev-seed.sh
fi
