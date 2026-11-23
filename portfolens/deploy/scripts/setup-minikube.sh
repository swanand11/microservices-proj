#!/bin/sh
set -eu
command -v minikube >/dev/null || { echo 'Install minikube first.' >&2; exit 1; }
minikube start --cpus=6 --memory=8192
minikube addons enable ingress
minikube addons enable metrics-server
"$(dirname "$0")/build-images.sh"
minikube image load portfolens-gateway:local
minikube image load portfolens-analytics:local
minikube image load portfolens-market-data:local
minikube image load portfolens-workflow-coordinator:local
minikube image load portfolens-frontend:local
kubectl apply -f "$(dirname "$0")/../../k8s/namespace.yaml"
