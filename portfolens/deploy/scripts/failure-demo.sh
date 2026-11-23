#!/bin/sh
set -eu
namespace=portfolens
echo 'Self-healing: deleting one gateway pod and waiting for a replacement.'
pod="$(kubectl get pod -n "$namespace" -l app=api-gateway -o jsonpath='{.items[0].metadata.name}')"
kubectl delete pod "$pod" -n "$namespace"
kubectl rollout status deployment/api-gateway -n "$namespace" --timeout=120s
echo 'Scaling gateway from 2 to 4 replicas.'
kubectl scale deployment/api-gateway -n "$namespace" --replicas=4
kubectl rollout status deployment/api-gateway -n "$namespace" --timeout=120s
echo 'Coordinator recovery is owner-assisted if an interrupted workflow needs the caller JWT.'
echo 'Read its journal and resume with authenticated POST /api/v1/sagas/{id}/recover.'
