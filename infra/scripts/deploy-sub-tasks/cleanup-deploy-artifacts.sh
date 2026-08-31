#!/usr/bin/env bash

set -euo pipefail

require_environment() {
  : "${EC2_DEPLOY_PATH:?EC2_DEPLOY_PATH 환경변수가 필요합니다.}"
}

normalize_deploy_path() {
  case "$EC2_DEPLOY_PATH" in
    "~") EC2_DEPLOY_PATH="$HOME" ;;
    "~/"*) EC2_DEPLOY_PATH="$HOME/${EC2_DEPLOY_PATH#~/}" ;;
  esac
}

remove_deploy_scripts() {
  rm -f \
    "${EC2_DEPLOY_PATH}/infra/scripts/deploy-imhere.sh"
}

remove_tls_scripts() {
  rm -rf "${EC2_DEPLOY_PATH}/infra/scripts/deploy-sub-tasks"
}

cleanup_deploy_artifacts() {
  remove_deploy_scripts
  remove_tls_scripts
}

main() {
  require_environment
  normalize_deploy_path
  cleanup_deploy_artifacts
  echo "배포용 스크립트 정리가 완료되었습니다."
}

main "$@"
