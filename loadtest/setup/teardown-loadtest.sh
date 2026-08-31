#!/usr/bin/env bash

# ImHere 부하 테스트 AWS 환경 삭제 스크립트
#
# CloudFormation 스택을 삭제하면 스택이 생성한 다음 리소스가 함께 삭제된다.
#   - VPC, Subnet, Route Table, Internet Gateway
#   - 앱, DB, 관측성 EC2
#   - 모든 부하 테스트용 Security Group
#
# EC2가 삭제되므로 해당 호스트에 복사한 Docker 컨테이너, 설정, 데이터도
# 함께 접근할 수 없게 된다. 로컬의 fixture와 결과 파일은 삭제하지 않는다.

set -euo pipefail

export AWS_CLI_FILE_ENCODING="${AWS_CLI_FILE_ENCODING:-UTF-8}"

AWS_REGION="${AWS_REGION:-ap-northeast-2}"
STACK_NAME="${STACK_NAME:-imhere-loadtest}"
CONFIRM_DELETE="false"
STATE_FILE="${STATE_FILE:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.loadtest-state}"

usage() {
  cat <<'USAGE'
Usage: ./loadtest/setup/teardown-loadtest.sh [--yes]

Options:
  --yes   삭제 확인을 생략하고 CloudFormation 스택을 삭제한다.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --yes) CONFIRM_DELETE="true"; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 1 ;;
  esac
done

command -v aws >/dev/null 2>&1 || { echo "aws CLI is required" >&2; exit 1; }

# 구성 스크립트가 기록한 상태에서 이번 실행의 Key Pair 정보를 읽는다.
if [[ -f "${STATE_FILE}" ]]; then
  # shellcheck disable=SC1090
  source "${STATE_FILE}"
fi

AWS_REGION="${AWS_REGION:-${STATE_AWS_REGION:-ap-northeast-2}}"
STACK_NAME="${STACK_NAME:-${STATE_STACK_NAME:-imhere-loadtest}}"
KEY_NAME="${KEY_NAME:-${STATE_KEY_NAME:-}}"
SSH_KEY_PATH="${SSH_KEY_PATH:-${STATE_SSH_KEY_PATH:-}}"

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

STACK_STATUS="$(aws cloudformation describe-stacks \
  --region "${AWS_REGION}" \
  --stack-name "${STACK_NAME}" \
  --query 'Stacks[0].StackStatus' \
  --output text 2>/dev/null || true)"

if [[ -z "${STACK_STATUS}" || "${STACK_STATUS}" == "None" ]]; then
  # CloudFormation 배포 전에 실패한 경우에도 이미 생성된 Key Pair를 정리한다.
  delete_key_pair
  echo "CloudFormation stack does not exist: ${STACK_NAME}"
  exit 0
fi

echo "Target stack: ${STACK_NAME}"
echo "Region: ${AWS_REGION}"
echo "Current status: ${STACK_STATUS}"

if [[ "${CONFIRM_DELETE}" != "true" ]]; then
  read -r -p "Delete this entire load-test environment? [y/N] " ANSWER
  [[ "${ANSWER}" == 'y' || "${ANSWER}" == 'Y' ]] || {
    echo "Deletion cancelled."
    exit 0
  }
fi

aws cloudformation delete-stack \
  --region "${AWS_REGION}" \
  --stack-name "${STACK_NAME}"

aws cloudformation wait stack-delete-complete \
  --region "${AWS_REGION}" \
  --stack-name "${STACK_NAME}"

delete_key_pair

echo "Load-test environment deleted: ${STACK_NAME}"
