#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
source "${SCRIPT_DIR}/01-env.sh"

eksctl delete cluster --name "${CLUSTER_NAME}" --region "${AWS_REGION}"
aws ecr batch-delete-image \
    --repository-name "${ECR_REPO}" \
    --image-ids imageTag="${IMAGE_TAG}" imageTag=latest \
    --region "${AWS_REGION}" || true
