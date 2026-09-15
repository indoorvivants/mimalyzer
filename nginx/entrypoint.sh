#!/usr/bin/env bash

set -xeuo pipefail

/run/app/bin/backend server --port 8080 &
nginx -g "daemon off;" &

wait -n

exit $?
