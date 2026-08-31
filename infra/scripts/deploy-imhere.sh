#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SUB_TASK_DIR="$SCRIPT_DIR/deploy-sub-tasks"
TLS_SCRIPT="$SUB_TASK_DIR/tls-sub-tasks/do-tls.sh"

require_environment() {
  : "${EC2_DEPLOY_PATH:?EC2_DEPLOY_PATH 환경변수가 필요합니다.}"
  : "${ECR_REGISTRY:?ECR_REGISTRY 환경변수가 필요합니다.}"
  : "${ECR_REPOSITORY:?ECR_REPOSITORY 환경변수가 필요합니다.}"
}

run_tls_bootstrap() {
  bash "$TLS_SCRIPT" bootstrap
}

run_docker_deploy() {
  bash "$SUB_TASK_DIR/deploy-imhere-with-docker.sh"
}

run_tls_issue() {
  bash "$TLS_SCRIPT" issue
}

run_healthcheck() {
  bash "$SUB_TASK_DIR/remote-healthcheck.sh"
}

run_cleanup() {
  bash "$SUB_TASK_DIR/cleanup-deploy-artifacts.sh"
}

main() {
  require_environment
  run_tls_bootstrap
  run_docker_deploy
  run_tls_issue
  run_healthcheck
  run_cleanup
}

main "$@"
