#!/usr/bin/env bash

set -euo pipefail

term() {
  kill -TERM "${backend_pid-}" "${nginx_pid-}" 2>/dev/null || true
}
trap term TERM INT

/run/app/bin/backend server --port 8080 &
backend_pid=$!

nginx -g "daemon off;" &
nginx_pid=$!

wait -n "$backend_pid" "$nginx_pid"
status=$?

term
wait || true
exit "$status"
