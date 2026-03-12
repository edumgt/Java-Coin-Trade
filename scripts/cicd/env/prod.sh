#!/usr/bin/env bash
set -euo pipefail

export APP_ENV=prod
export DEPLOY_MODE=eks
export AWS_REGION="${AWS_REGION:-ap-northeast-2}"
export CLUSTER_NAME="${CLUSTER_NAME:-crypto-mock-prod}"
export APP_NAMESPACE="${APP_NAMESPACE:-crypto-mock}"
export ECR_REPO="${ECR_REPO:-java-crypto-mock-prod}"
export KUSTOMIZE_DIR=k8s/eks
export IMAGE_TAG="${IMAGE_TAG:-prod-$(date +%Y%m%d%H%M%S)}"
