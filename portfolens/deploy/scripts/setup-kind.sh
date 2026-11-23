#!/bin/sh
set -eu
command -v kind >/dev/null || { echo 'Install kind first.' >&2; exit 1; }
command -v kubectl >/dev/null || { echo 'Install kubectl first.' >&2; exit 1; }
root="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
kind create cluster --name portfolens --config "$root/k8s/kind-config.yaml" || true
kubectl apply -f "$root/k8s/namespace.yaml"
"$root/deploy/scripts/build-images.sh"
kind load docker-image portfolens-gateway:local --name portfolens
kind load docker-image portfolens-analytics:local --name portfolens
kind load docker-image portfolens-market-data:local --name portfolens
kind load docker-image portfolens-workflow-coordinator:local --name portfolens
kind load docker-image portfolens-frontend:local --name portfolens
kubectl apply -f "$root/k8s/namespace.yaml"
