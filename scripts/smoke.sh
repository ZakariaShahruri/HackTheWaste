#!/usr/bin/env bash
# Checks a running backend from the outside: health, the seeded demo data, and
# the ML service behind it. Used by CI against compose and after a deploy.
#   scripts/smoke.sh [backend-url]    (default http://localhost:8080)
set -euo pipefail

base="${1:-http://localhost:8080}"
base="${base%/}"

check() {
  local name=$1 path=$2 expected=$3 body
  if ! body=$(curl -fsS --max-time 30 "$base$path"); then
    echo "FAIL $name: $path did not answer 2xx"
    exit 1
  fi
  if ! grep -qF -- "$expected" <<<"$body"; then
    echo "FAIL $name: $path has no $expected"
    exit 1
  fi
  echo "ok   $name"
}

check health      "/actuator/health"                    '"status":"UP"'
check stats       "/api/stats"                          '"totalReports"'
check work-orders "/api/work-orders"                    '"planonRef"'
check feed        "/api/feed"                           '"photoUrl"'
check clans       "/api/leaderboard"                    '"shortCode"'
check predictions "/api/predictions/grid?day=4&hour=22" '"available":true'
