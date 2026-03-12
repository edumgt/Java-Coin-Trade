#!/usr/bin/env bash
set -euo pipefail

export APP_ENV=local
export DEPLOY_MODE=docker-compose
export IMAGE_TAG="${IMAGE_TAG:-local-$(date +%Y%m%d%H%M%S)}"
