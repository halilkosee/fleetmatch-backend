#!/usr/bin/env bash
set -euo pipefail

APP_DIR="${APP_DIR:-/opt/easyfleetmatch/backend}"
BRANCH="${BRANCH:-develop}"
ENV_FILE="${ENV_FILE:-.env.dev}"

cd "$APP_DIR"

git fetch origin --prune
git checkout "$BRANCH"
git pull --ff-only origin "$BRANCH"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "$ENV_FILE is missing in $APP_DIR" >&2
  exit 1
fi

scripts/dev-stack.sh restart
scripts/dev-stack.sh smoke
