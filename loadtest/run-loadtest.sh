#!/usr/bin/env bash

set -Eeuo pipefail

# 부하 테스트 전체 수명주기를 실행한다.
#
# 실행 순서:
#   1. AWS 부하 테스트 인프라 생성
#   2. k6 테스트 데이터 및 JWT 생성
#   3. 테스트 시나리오 선택
#   4. k6 부하 테스트 실행
#   5. AWS 인프라 및 EC2 키 페어 삭제

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
SETUP_SCRIPT="${REPO_ROOT}/loadtest/setup/setup-loadtest.sh"
TEARDOWN_SCRIPT="${REPO_ROOT}/loadtest/setup/teardown-loadtest.sh"
INIT_DIR="${REPO_ROOT}/loadtest/k6/init"
TEST_DIR="${REPO_ROOT}/loadtest/k6/test"
GENERATED_DIR="${REPO_ROOT}/loadtest/k6/generated"
FIXTURE_PATH="${GENERATED_DIR}/tokens.json"
RESULT_DIR="${GENERATED_DIR}/results"

AWS_REGION="${AWS_REGION:-ap-northeast-2}"
STACK_NAME="${STACK_NAME:-imhere-loadtest}"
BASE_URL="${BASE_URL:-}"
TEST_PLAN="${TEST_PLAN:-precision}"
TARGET_RPS="${TARGET_RPS:-100}"
STAGE_DURATION="${STAGE_DURATION:-3m}"
K6_PATH="${K6_PATH:-k6}"

cleanup() {
  local exit_code=$?

  trap - EXIT
  echo
  echo "[5/5] Removing load-test resources"

  if [[ -x "${TEARDOWN_SCRIPT}" ]]; then
    if ! "${TEARDOWN_SCRIPT}" --yes; then
      echo "teardown failed; inspect AWS resources manually." >&2
      exit_code=1
    fi
  else
    echo "teardown script not found: ${TEARDOWN_SCRIPT}" >&2
    exit_code=1
  fi

  exit "${exit_code}"
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "required command not found: $1" >&2
    exit 1
  }
}

require_file() {
  [[ -f "$1" ]] || {
    echo "required file not found: $1" >&2
    exit 1
  }
}

select_test() {
  local -a test_paths=()
  local index=1
  local selected

  while IFS= read -r -d '' path; do
    test_paths+=("${path}")
  done < <(find "${TEST_DIR}" -type f -name '*.js' -print0 | sort -z)

  [[ "${#test_paths[@]}" -gt 0 ]] || {
    echo "no k6 test scripts found: ${TEST_DIR}" >&2
    exit 1
  }

  echo ""
  echo "Available k6 tests:"
  for path in "${test_paths[@]}"; do
    printf '  %d) %s\n' "${index}" "${path#"${TEST_DIR}/"}"
    index=$((index + 1))
  done

  while true; do
    read -r -p "Select a test [1-${#test_paths[@]}]: " selected
    if [[ "${selected}" =~ ^[0-9]+$ ]] &&
       (( selected >= 1 && selected <= ${#test_paths[@]} )); then
      SELECTED_TEST="${test_paths[selected - 1]}"
      break
    fi
    echo "Please enter a number between 1 and ${#test_paths[@]}." >&2
  done
}

trap cleanup EXIT

require_command aws
require_command node
require_command curl
require_file "${SETUP_SCRIPT}"
require_file "${TEARDOWN_SCRIPT}"
require_file "${INIT_DIR}/generate-test-data.mjs"
require_file "${INIT_DIR}/issue-jwt.mjs"

echo "[1/5] Creating AWS load-test environment"
"${SETUP_SCRIPT}"

echo "[2/5] Generating k6 test data and JWTs"
node "${INIT_DIR}/generate-test-data.mjs" "${GENERATED_DIR}"
node "${INIT_DIR}/issue-jwt.mjs" \
  "${GENERATED_DIR}/fixture.json" \
  "${FIXTURE_PATH}"

require_file "${FIXTURE_PATH}"

if [[ -z "${BASE_URL}" ]]; then
  APP_PUBLIC_IP="$(aws cloudformation describe-stacks \
    --stack-name "${STACK_NAME}" \
    --region "${AWS_REGION}" \
    --query "Stacks[0].Outputs[?OutputKey=='AppPublicIp'].OutputValue | [0]" \
    --output text)"
  [[ -n "${APP_PUBLIC_IP}" && "${APP_PUBLIC_IP}" != "None" ]] || {
    echo "AppPublicIp could not be resolved from stack: ${STACK_NAME}" >&2
    exit 1
  }
  BASE_URL="http://${APP_PUBLIC_IP}"
fi

select_test

read -r -p "Test plan [precision/single/breakpoint] (${TEST_PLAN}): " input_plan
TEST_PLAN="${input_plan:-${TEST_PLAN}}"
read -r -p "Target RPS (${TARGET_RPS}): " input_rps
TARGET_RPS="${input_rps:-${TARGET_RPS}}"
read -r -p "Stage duration (${STAGE_DURATION}): " input_duration
STAGE_DURATION="${input_duration:-${STAGE_DURATION}}"

require_command "${K6_PATH}"
mkdir -p "${RESULT_DIR}"

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
TEST_NAME="$(basename "${SELECTED_TEST}" .js)"
RESULT_PATH="${RESULT_DIR}/${TEST_NAME}-${TEST_PLAN}-${TIMESTAMP}.json"
TEST_TAG="imhere-${TEST_NAME}-${TEST_PLAN}-${TIMESTAMP}"

echo "[4/5] Running k6 test"
echo "Test: ${SELECTED_TEST#"${TEST_DIR}/"}"
echo "Base URL: ${BASE_URL}"
echo "Plan: ${TEST_PLAN}, target RPS: ${TARGET_RPS}, duration: ${STAGE_DURATION}"

"${K6_PATH}" run --quiet \
  --summary-export "${RESULT_PATH}" \
  --tag "test=${TEST_TAG}" \
  -e "BASE_URL=${BASE_URL}" \
  -e "FIXTURE=${FIXTURE_PATH}" \
  -e "SCENARIO=${TEST_NAME}" \
  -e "TEST_PLAN=${TEST_PLAN}" \
  -e "TARGET_RPS=${TARGET_RPS}" \
  -e "STAGE_DURATION=${STAGE_DURATION}" \
  -e 'INSECURE_TLS=true' \
  "${SELECTED_TEST}"

require_file "${RESULT_PATH}"
echo "k6 completed successfully: ${RESULT_PATH}"
