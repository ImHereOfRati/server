#!/usr/bin/env bash

set -Eeuo pipefail

# AI가 후보별 이미지를 준비한 뒤 동일한 격리 AWS 환경을 반복 실행하는
# 비대화형 실행기다. 후보 이미지 태그는 쉼표로 전달한다.
#
# IMAGE_TAGS=latest,candidate-a,candidate-b \
#   MYSQL_ROOT_PASSWORD='...' \
#   ./loadtest/run-ai-optimization.sh

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
RUN_ID="${RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)-$$}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
BASE_STACK_NAME="${STACK_NAME:-imhere-loadtest-ai}"
TEST_FILE="${TEST_FILE:-many-people-many-request-test/mixed-workload.js}"
TEST_PLAN="${TEST_PLAN:-single}"
TARGET_RPS="${TARGET_RPS:-300}"
STAGE_DURATION="${STAGE_DURATION:-10m}"
REPORT_ROOT="${REPORT_ROOT:-${REPO_ROOT}/docs/loadtest/300-tps/runs/${RUN_ID}}"
MAX_CANDIDATES="${MAX_CANDIDATES:-6}"
IFS=',' read -r -a IMAGE_TAG_ARRAY <<< "${IMAGE_TAGS:-latest}"

mkdir -p "${REPORT_ROOT}"

require_command() {
  command -v "$1" >/dev/null 2>&1 || { echo "required command not found: $1" >&2; exit 1; }
}

require_command aws
require_command node
if [[ -z "${MYSQL_ROOT_PASSWORD:-}" ]]; then
  echo "MYSQL_ROOT_PASSWORD must be provided for non-interactive execution." >&2
  exit 2
fi

if (( ${#IMAGE_TAG_ARRAY[@]} > MAX_CANDIDATES )); then
  echo "IMAGE_TAGS contains more than MAX_CANDIDATES=${MAX_CANDIDATES}" >&2
  exit 2
fi

summary_path="${REPORT_ROOT}/suite.md"
{
  echo "# AI optimization suite"
  echo
  echo "- Run ID: ${RUN_ID}"
  echo "- Target: ${TARGET_RPS} TPS for ${STAGE_DURATION}"
  echo "- Test: ${TEST_FILE} (${TEST_PLAN})"
  echo
  echo '| Candidate | Result | Report | SQL report |'
  echo '|---|---|---|---|'
} > "${summary_path}"

for index in "${!IMAGE_TAG_ARRAY[@]}"; do
  candidate_no=$((index + 1))
  candidate_tag="${IMAGE_TAG_ARRAY[index]}"
  candidate_id="candidate-${candidate_no}-${candidate_tag//[^a-zA-Z0-9._-]/-}"
  stack_name="${BASE_STACK_NAME}-${RUN_ID}-${candidate_no}"
  state_file="${REPO_ROOT}/loadtest/setup/.ai-${RUN_ID}-${candidate_no}.state"
  key_dir="${REPO_ROOT}/loadtest/setup/info/keys/ai-${RUN_ID}-${candidate_no}"
  candidate_report_dir="${REPORT_ROOT}/${candidate_id}"
  mkdir -p "${candidate_report_dir}"

  echo "[${candidate_no}/${#IMAGE_TAG_ARRAY[@]}] ${candidate_tag}"
  status=0
  if env \
    NON_INTERACTIVE=true \
    KEEP_LOADTEST_RESOURCES=true \
    STACK_NAME="${stack_name}" \
    IMAGE_TAG="${candidate_tag}" \
    TEST_FILE="${TEST_FILE}" \
    TEST_PLAN="${TEST_PLAN}" \
    TARGET_RPS="${TARGET_RPS}" \
    STAGE_DURATION="${STAGE_DURATION}" \
    REPORT_DIR="${candidate_report_dir}" \
    STATE_FILE="${state_file}" \
    KEY_DIR="${key_dir}" \
    "${REPO_ROOT}/loadtest/run-loadtest.sh"; then
    status=0
  else
    status=$?
  fi

  report_path="$(find "${candidate_report_dir}" -maxdepth 1 -type f -name '*.md' -print -quit)"
  [[ -n "${report_path}" ]] || report_path="${candidate_report_dir}/missing-k6-report.md"
  if [[ ! -f "${report_path}" ]]; then
    printf '# Candidate %s\n\nThe k6 run exited with status %s before creating a report.\n' "${candidate_tag}" "${status}" > "${report_path}"
  fi

  sql_report="${candidate_report_dir}/sql-optimizer.md"
  sql_status="not-collected"
  if [[ -f "${state_file}" ]]; then
    # shellcheck disable=SC1090
    source "${state_file}"
    db_ip="$(aws cloudformation describe-stacks --region "${AWS_REGION}" --stack-name "${stack_name}" --query "Stacks[0].Outputs[?OutputKey=='DbPublicIp'].OutputValue | [0]" --output text 2>/dev/null || true)"
    if [[ -n "${db_ip}" && "${db_ip}" != "None" && -n "${MYSQL_ROOT_PASSWORD:-}" ]]; then
      if bash "${REPO_ROOT}/loadtest/tools/collect-sql-report.sh" \
        --db-public-ip "${db_ip}" --ssh-key "${SSH_KEY_PATH}" \
        --password "${MYSQL_ROOT_PASSWORD}" --output "${sql_report}"; then
        sql_status="collected"
      else
        sql_status="collection-failed"
      fi
    fi
  fi

  if [[ -f "${state_file}" ]]; then
    STACK_NAME="${stack_name}" STATE_FILE="${state_file}" AWS_REGION="${AWS_REGION}" \
      bash "${REPO_ROOT}/loadtest/setup/teardown-loadtest.sh" --yes || true
  fi
  rm -rf -- "${key_dir}" "${state_file}"
  printf '| %s | %s | [%s](%s) | %s |\n' "${candidate_tag}" "${status}" "${candidate_id}" "${candidate_id}/$(basename "${report_path}")" "${sql_status}" >> "${summary_path}"
done

echo "AI optimization suite Markdown: ${summary_path}"
