#!/bin/sh
set -eu
root="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
docker build -t portfolens-gateway:local "$root/backend"
docker build -t portfolens-analytics:local "$root/analytics-service"
docker build -t portfolens-market-data:local "$root/market-data-service"
docker build -t portfolens-workflow-coordinator:local "$root/workflow-coordinator-service"
docker build -t portfolens-frontend:local "$root/frontend"
