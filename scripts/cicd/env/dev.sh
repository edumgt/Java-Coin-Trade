#!/usr/bin/env bash
set -euo pipefail

export APP_ENV=dev
export DEPLOY_MODE=k8s-dev
export KUSTOMIZE_DIR=k8s/overlays/dev
export K8S_NAMESPACE=crypto-mock-dev
export IMAGE_TAG="${IMAGE_TAG:-dev-$(date +%Y%m%d%H%M%S)}"
