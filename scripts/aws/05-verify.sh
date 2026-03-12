#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
source "${SCRIPT_DIR}/01-env.sh"

kubectl -n "${APP_NAMESPACE}" get pods
kubectl -n "${APP_NAMESPACE}" get svc
kubectl -n "${APP_NAMESPACE}" get ingress

echo "ALB DNS:"
kubectl -n "${APP_NAMESPACE}" get ingress crypto-mock-ingress \
    -o jsonpath='{.status.loadBalancer.ingress[0].hostname}' || true
echo
