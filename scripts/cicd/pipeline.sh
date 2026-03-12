#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
TARGET_ENV="${1:-local}"

"${SCRIPT_DIR}/ci.sh" "${TARGET_ENV}"
"${SCRIPT_DIR}/deploy.sh" "${TARGET_ENV}"
