#!/usr/bin/env bash
set -euo pipefail

export APP_ENV=stage
export DEPLOY_MODE=eks
export AWS_REGION="${AWS_REGION:-ap-northeast-2}"
export CLUSTER_NAME="${CLUSTER_NAME:-crypto-mock-stage}"
export APP_NAMESPACE="${APP_NAMESPACE:-crypto-mock}"
export ECR_REPO="${ECR_REPO:-java-crypto-mock-stage}"
export KUSTOMIZE_DIR=k8s/eks
export IMAGE_TAG="${IMAGE_TAG:-stage-$(date +%Y%m%d%H%M%S)}"
