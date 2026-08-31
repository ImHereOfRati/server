#!/usr/bin/env bash

set -euo pipefail

export AWS_CLI_FILE_ENCODING="${AWS_CLI_FILE_ENCODING:-UTF-8}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
STACK_NAME="${STACK_NAME:-imhere-loadtest}"
CONFIRM_DELETE="false"
STATE_FILE="${STATE_FILE:-${SCRIPT_DIR}/.loadtest-state}"

usage() {
  cat <<'USAGE'
Usage: ./loadtest/setup/teardown-loadtest.sh [--yes]

Options:
  --yes   삭제 확인 없이 CloudFormation 스택을 삭제합니다.
USAGE
}

parse_arguments() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --yes) CONFIRM_DELETE="true"; shift ;;
      -h|--help) usage; exit 0 ;;
      *) echo "알 수 없는 옵션: $1" >&2; usage >&2; exit 1 ;;
    esac
  done
}

require_commands() {
  command -v aws >/dev/null 2>&1 || {
    echo "필수 명령이 없습니다: aws" >&2
    exit 1
  }
}

load_state() {
  if [[ -f "${STATE_FILE}" ]]; then
    # shellcheck disable=SC1090
    source "${STATE_FILE}"
  fi

  AWS_REGION="${AWS_REGION:-${STATE_AWS_REGION:-ap-northeast-2}}"
  STACK_NAME="${STACK_NAME:-${STATE_STACK_NAME:-imhere-loadtest}}"
  KEY_NAME="${KEY_NAME:-${STATE_KEY_NAME:-}}"
  SSH_KEY_PATH="${SSH_KEY_PATH:-${STATE_SSH_KEY_PATH:-}}"
}

read_stack_status() {
  aws cloudformation describe-stacks \
    --region "${AWS_REGION}" \
    --stack-name "${STACK_NAME}" \
    --query 'Stacks[0].StackStatus' \
    --output text 2>/dev/null || true
}

delete_key_pair() {
  if [[ -n "${KEY_NAME}" ]]; then
    aws ec2 delete-key-pair \
      --region "${AWS_REGION}" \
      --key-name "${KEY_NAME}" 2>/dev/null || true
  fi

  if [[ -n "${SSH_KEY_PATH}" && -f "${SSH_KEY_PATH}" ]]; then
    rm -f -- "${SSH_KEY_PATH}"
  fi
  rm -f -- "${STATE_FILE}"
}

confirm_deletion() {
  echo "대상 스택: ${STACK_NAME}"
  echo "리전: ${AWS_REGION}"
  echo "현재 상태: ${STACK_STATUS}"

  if [[ "${CONFIRM_DELETE}" != "true" ]]; then
    read -r -p "부하 테스트 환경 전체를 삭제할까요? [y/N] " ANSWER
    [[ "${ANSWER}" == 'y' || "${ANSWER}" == 'Y' ]] || {
      echo "삭제를 취소했습니다."
      exit 0
    }
  fi
}

delete_stack() {
  aws cloudformation delete-stack \
    --region "${AWS_REGION}" \
    --stack-name "${STACK_NAME}"

  aws cloudformation wait stack-delete-complete \
    --region "${AWS_REGION}" \
    --stack-name "${STACK_NAME}"
}

main() {
  parse_arguments "$@"
  require_commands
  load_state

  STACK_STATUS="$(read_stack_status)"
  if [[ -z "${STACK_STATUS}" || "${STACK_STATUS}" == "None" ]]; then
    delete_key_pair
    echo "CloudFormation 스택이 없습니다: ${STACK_NAME}"
    exit 0
  fi

  confirm_deletion
  delete_stack
  delete_key_pair
  echo "부하 테스트 환경을 삭제했습니다: ${STACK_NAME}"
}

main "$@"
