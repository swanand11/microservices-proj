#!/bin/sh
set -eu
root="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
kubectl apply -k "$root/k8s"
kubectl rollout status deployment/api-gateway -n portfolens --timeout=180s
kubectl rollout status deployment/frontend -n portfolens --timeout=180s
