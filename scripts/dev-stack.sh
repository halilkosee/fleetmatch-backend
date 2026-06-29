#!/usr/bin/env bash
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env.dev}"
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.dev.yml}"
BASE_URL="${BASE_URL:-http://localhost:8080}"
COMMAND="${1:-up}"

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

wait_for_backend() {
  printf 'Waiting for backend readiness at %s\n' "$BASE_URL/actuator/health/readiness"
  for _ in $(seq 1 60); do
    if curl -fsS "$BASE_URL/actuator/health/readiness" >/dev/null; then
      printf 'Backend is ready.\n'
      return 0
    fi
    sleep 2
  done

  printf 'Backend did not become ready in time.\n' >&2
  compose logs --tail=120 backend >&2 || true
  return 1
}

case "$COMMAND" in
  up)
    compose up -d --build
    wait_for_backend
    ;;
  down)
    compose down
    ;;
  restart)
    compose up -d --build
    wait_for_backend
    ;;
  logs)
    compose logs -f "${2:-backend}"
    ;;
  status)
    compose ps
    curl -fsS "$BASE_URL/api/health"
    printf '\n'
    ;;
  smoke)
    curl -fsS "$BASE_URL/api/health"
    printf '\n'
    curl -fsS "$BASE_URL/actuator/health/readiness"
    printf '\n'
    ;;
  *)
    echo "Usage: $0 {up|down|restart|logs|status|smoke}" >&2
    exit 1
    ;;
esac
