#!/bin/sh
set -eu
root="$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)"
kubectl apply -f "$root/k8s/services.yaml"
kubectl get service frontend -n portfolens -o wide
