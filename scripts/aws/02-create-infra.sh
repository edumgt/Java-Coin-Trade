#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
source "${SCRIPT_DIR}/01-env.sh"

if ! aws ecr describe-repositories \
    --repository-names "${ECR_REPO}" \
    --region "${AWS_REGION}" >/dev/null 2>&1; then
    aws ecr create-repository \
        --repository-name "${ECR_REPO}" \
        --image-scanning-configuration scanOnPush=true \
        --region "${AWS_REGION}" >/dev/null
fi

if ! eksctl get cluster --name "${CLUSTER_NAME}" --region "${AWS_REGION}" >/dev/null 2>&1; then
    eksctl create cluster \
        --name "${CLUSTER_NAME}" \
        --region "${AWS_REGION}" \
        --version 1.30 \
        --nodegroup-name main-ng \
        --node-type t3.medium \
        --nodes 2 \
        --nodes-min 2 \
        --nodes-max 4 \
        --managed
fi

aws eks update-kubeconfig \
    --name "${CLUSTER_NAME}" \
    --region "${AWS_REGION}" >/dev/null

echo "EKS infra is ready."
echo "AWS Load Balancer Controller and Route53/TLS are managed separately."
