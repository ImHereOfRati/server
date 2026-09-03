#!/usr/bin/env bash

set -euo pipefail

export AWS_CLI_FILE_ENCODING="${AWS_CLI_FILE_ENCODING:-UTF-8}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
STACK_NAME="${STACK_NAME:-imhere-loadtest}"
PROD_STACK_NAME="${PROD_STACK_NAME:-imhere-prod-infra}"
EC2_USER="${EC2_USER:-ec2-user}"
REMOTE_APP_ROOT="${REMOTE_APP_ROOT:-/opt/imhere}"
REMOTE_OBSERVABILITY_ROOT="${REMOTE_OBSERVABILITY_ROOT:-/opt/imhere-observability}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
KEY_DIR="${KEY_DIR:-${SCRIPT_DIR}/info/keys}"
STATE_FILE="${STATE_FILE:-${SCRIPT_DIR}/.loadtest-state}"

CFN_TEMPLATE="${REPO_ROOT}/loadtest/setup/infra/cloudformation/load-test-aws-setup.yaml"
MYSQL_SCRIPT="${REPO_ROOT}/loadtest/setup/infra/database/setup-mysql.sh"
MYSQL_SCHEMA_SCRIPT="${REPO_ROOT}/db/init/mysql/imhere-full-init.sql"
MYSQL_SEED_SCRIPT="${LOADTEST_SEED_PATH:-${REPO_ROOT}/loadtest/k6/generated/seed.sql}"
TEST_ENV_DIR="${REPO_ROOT}/loadtest/setup/test-env"
INFRA_DIR="${REPO_ROOT}/loadtest/setup/infra"
TEMP_ENV_DIR=""

require_commands() {
  local command_name
  for command_name in aws ssh scp curl; do
    command -v "${command_name}" >/dev/null 2>&1 || {
      echo "필수 명령이 없습니다: ${command_name}" >&2
      exit 1
    }
  done
}

validate_files() {
  [[ -f "${MYSQL_SCHEMA_SCRIPT}" ]] || { echo "MySQL 스키마 파일이 없습니다: ${MYSQL_SCHEMA_SCRIPT}" >&2; exit 1; }
  [[ -f "${CFN_TEMPLATE}" ]] || { echo "CloudFormation 템플릿이 없습니다: ${CFN_TEMPLATE}" >&2; exit 1; }
  [[ -f "${MYSQL_SCRIPT}" ]] || { echo "MySQL 설정 스크립트가 없습니다: ${MYSQL_SCRIPT}" >&2; exit 1; }
  [[ -d "${TEST_ENV_DIR}" ]] || { echo "테스트 환경 디렉터리가 없습니다: ${TEST_ENV_DIR}" >&2; exit 1; }
}

create_key_pair() {
  mkdir -p "${KEY_DIR}"
  umask 077
  KEY_NAME="imhere-loadtest-$(date +%Y%m%d%H%M%S)-$$"
  SSH_KEY_PATH="${KEY_DIR}/${KEY_NAME}.pem"

  aws ec2 create-key-pair \
    --region "${AWS_REGION}" \
    --key-name "${KEY_NAME}" \
    --query 'KeyMaterial' \
    --output text > "${SSH_KEY_PATH}"
  chmod 600 "${SSH_KEY_PATH}"

  cat > "${STATE_FILE}" <<STATE
AWS_REGION=${AWS_REGION}
STACK_NAME=${STACK_NAME}
KEY_NAME=${KEY_NAME}
SSH_KEY_PATH=${SSH_KEY_PATH}
STATE
}

resolve_network_settings() {
  if [[ -z "${LOAD_GENERATOR_CIDR:-}" ]]; then
    PUBLIC_IP="${PUBLIC_IP:-$(curl -fsSL https://checkip.amazonaws.com | tr -d '[:space:]')}"
    LOAD_GENERATOR_CIDR="${PUBLIC_IP}/32"
  fi
  OBSERVABILITY_ADMIN_CIDR="${OBSERVABILITY_ADMIN_CIDR:-${LOAD_GENERATOR_CIDR}}"
}

resolve_runtime_settings() {
  MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-}"
  if [[ -z "${MYSQL_ROOT_PASSWORD}" ]]; then
    read -r -s -p 'MySQL root password: ' MYSQL_ROOT_PASSWORD
    printf '\n' >&2
  fi
  validate_mysql_password "${MYSQL_ROOT_PASSWORD}"

  if [[ -z "${ECR_REPOSITORY:-}" ]]; then
    ECR_REPOSITORY="$(aws cloudformation describe-stacks \
      --stack-name "${PROD_STACK_NAME}" \
      --region "${AWS_REGION}" \
      --query "Stacks[0].Outputs[?OutputKey=='EcrRepositoryName'].OutputValue | [0]" \
      --output text)"
  fi
  ECR_REPOSITORY="${ECR_REPOSITORY:?ECR 저장소를 확인할 수 없습니다.}"
  AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:-$(aws sts get-caller-identity --query Account --output text)}"
  ECR_REGISTRY="${ECR_REGISTRY:-${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com}"
}

validate_mysql_password() {
  local password="$1"

  [[ "${#password}" -ge 8 \
    && "${password}" =~ [A-Z] \
    && "${password}" =~ [a-z] \
    && "${password}" =~ [0-9] \
    && "${password}" =~ [^[:alnum:]] ]] || {
    echo "MySQL root 비밀번호는 8자 이상이며 대문자·소문자·숫자·특수문자를 포함해야 합니다." >&2
    exit 1
  }
}

deploy_stack() {
  echo '[1/3] AWS 부하 테스트 스택을 생성합니다.'
  aws cloudformation deploy \
    --region "${AWS_REGION}" \
    --template-file "${CFN_TEMPLATE}" \
    --stack-name "${STACK_NAME}" \
    --parameter-overrides \
      "KeyName=${KEY_NAME}" \
      "LoadGeneratorCidr=${LOAD_GENERATOR_CIDR}" \
      "ObservabilityAdminCidr=${OBSERVABILITY_ADMIN_CIDR}"
}

stack_output() {
  aws cloudformation describe-stacks \
    --region "${AWS_REGION}" \
    --stack-name "${STACK_NAME}" \
    --query "Stacks[0].Outputs[?OutputKey=='$1'].OutputValue | [0]" \
    --output text
}

read_stack_outputs() {
  APP_PUBLIC_IP="$(stack_output AppPublicIp)"
  DB_PUBLIC_IP="$(stack_output DbPublicIp)"
  DB_PRIVATE_IP="$(stack_output DbPrivateIp)"
  OBSERVABILITY_PUBLIC_IP="$(stack_output ObservabilityPublicIp)"
  OBSERVABILITY_PRIVATE_IP="$(stack_output ObservabilityPrivateIp)"
  SSH_OPTIONS=(-i "${SSH_KEY_PATH}" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=10)
  # -n: detach remote command's stdin from ours so it can't drain input meant for
  # run-loadtest.sh's later interactive prompts (test selection, plan, RPS, duration).
  SSH_OPTIONS_NO_STDIN=("${SSH_OPTIONS[@]}" -n)
}

wait_for_ssh() {
  local host="$1"
  local attempt
  for attempt in $(seq 1 60); do
    if ssh "${SSH_OPTIONS_NO_STDIN[@]}" "${EC2_USER}@${host}" 'true' >/dev/null 2>&1; then
      return 0
    fi
    sleep 5
  done
  echo "SSH 연결 시간 초과: ${host}" >&2
  exit 1
}

configure_mysql() {
  echo '[2/3] MySQL을 설정합니다.'
  wait_for_ssh "${DB_PUBLIC_IP}"
  scp "${SSH_OPTIONS[@]}" "${MYSQL_SCRIPT}" "${EC2_USER}@${DB_PUBLIC_IP}:/tmp/setup-mysql.sh"
  # Normalize CRLF from Windows checkouts before Linux executes the shebang.
  ssh "${SSH_OPTIONS_NO_STDIN[@]}" "${EC2_USER}@${DB_PUBLIC_IP}" \
    "sudo sed -i 's/\\r$//' /tmp/setup-mysql.sh"
  ssh "${SSH_OPTIONS_NO_STDIN[@]}" "${EC2_USER}@${DB_PUBLIC_IP}" \
    'sudo install -m 0755 /tmp/setup-mysql.sh /opt/mysql/setup-mysql.sh'
  ssh "${SSH_OPTIONS_NO_STDIN[@]}" "${EC2_USER}@${DB_PUBLIC_IP}" \
    "sudo env MYSQL_ROOT_PASSWORD='${MYSQL_ROOT_PASSWORD}' /opt/mysql/setup-mysql.sh"
  initialize_mysql_schema
}

initialize_mysql_schema() {
  echo 'MySQL 스키마를 초기화합니다.'
  scp "${SSH_OPTIONS[@]}" "${MYSQL_SCHEMA_SCRIPT}" \
    "${EC2_USER}@${DB_PUBLIC_IP}:/tmp/imhere-full-init.sql"
  ssh "${SSH_OPTIONS_NO_STDIN[@]}" "${EC2_USER}@${DB_PUBLIC_IP}" \
    "sudo env MYSQL_PWD='${MYSQL_ROOT_PASSWORD}' mysql --protocol=socket -uroot rati < /tmp/imhere-full-init.sql"

  [[ -f "${MYSQL_SEED_SCRIPT}" ]] || {
    echo "load-test seed SQL not found: ${MYSQL_SEED_SCRIPT}" >&2
    exit 1
  }
  echo '부하테스트 fixture 데이터를 MySQL에 주입합니다.'
  scp "${SSH_OPTIONS[@]}" "${MYSQL_SEED_SCRIPT}" \
    "${EC2_USER}@${DB_PUBLIC_IP}:/tmp/loadtest-seed.sql"
  ssh "${SSH_OPTIONS_NO_STDIN[@]}" "${EC2_USER}@${DB_PUBLIC_IP}" \
    "sudo env MYSQL_PWD='${MYSQL_ROOT_PASSWORD}' mysql --protocol=socket -uroot rati < /tmp/loadtest-seed.sql"
}

prepare_runtime_env() {
  TEMP_ENV_DIR="$(mktemp -d)"
  trap 'rm -rf "${TEMP_ENV_DIR}"' EXIT

  sed "s|^DB_HOST=.*|DB_HOST=${DB_PRIVATE_IP}|; s|^DB_PASSWORD=.*|DB_PASSWORD=${MYSQL_ROOT_PASSWORD}|" \
    "${TEST_ENV_DIR}/server.env" > "${TEMP_ENV_DIR}/server.env"
  cp "${TEST_ENV_DIR}/nginx.env" "${TEMP_ENV_DIR}/nginx.env"
  sed "s|\${OBSERVABILITY_PRIVATE_IP}|${OBSERVABILITY_PRIVATE_IP}|g" \
    "${TEST_ENV_DIR}/alloy.env" > "${TEMP_ENV_DIR}/alloy.env"
}

deploy_application() {
  echo '[3/3] Spring Boot와 Nginx, Alloy를 설정합니다.'
  wait_for_ssh "${APP_PUBLIC_IP}"
  wait_for_ssh "${OBSERVABILITY_PUBLIC_IP}"

  prepare_runtime_env
  ssh "${SSH_OPTIONS_NO_STDIN[@]}" "${EC2_USER}@${APP_PUBLIC_IP}" \
    "mkdir -p '${REMOTE_APP_ROOT}/env' '${REMOTE_APP_ROOT}/secrets' '${REMOTE_APP_ROOT}/infra/nginx/certbot' '${REMOTE_APP_ROOT}/loadtest/setup'"
  scp "${SSH_OPTIONS[@]}" "${REPO_ROOT}/docker-compose.yml" \
    "${EC2_USER}@${APP_PUBLIC_IP}:${REMOTE_APP_ROOT}/docker-compose.yml"
  scp "${SSH_OPTIONS[@]}" "${TEMP_ENV_DIR}"/*.env \
    "${EC2_USER}@${APP_PUBLIC_IP}:${REMOTE_APP_ROOT}/env/"
  scp "${SSH_OPTIONS[@]}" "${TEST_ENV_DIR}/imhereFirebaseKey.json" \
    "${EC2_USER}@${APP_PUBLIC_IP}:${REMOTE_APP_ROOT}/secrets/imhereFirebaseKey.json"
  scp "${SSH_OPTIONS[@]}" "${TEST_ENV_DIR}/nginx.conf" \
    "${EC2_USER}@${APP_PUBLIC_IP}:${REMOTE_APP_ROOT}/infra/nginx/nginx.conf"
  scp -r "${SSH_OPTIONS[@]}" "${INFRA_DIR}" \
    "${EC2_USER}@${APP_PUBLIC_IP}:${REMOTE_APP_ROOT}/loadtest/setup/"

  aws ecr get-login-password --region "${AWS_REGION}" \
    | ssh "${SSH_OPTIONS[@]}" "${EC2_USER}@${APP_PUBLIC_IP}" \
      "sudo docker login --username AWS --password-stdin '${ECR_REGISTRY}'"
  ssh "${SSH_OPTIONS_NO_STDIN[@]}" "${EC2_USER}@${APP_PUBLIC_IP}" \
    "cd '${REMOTE_APP_ROOT}' && sudo env ECR_REGISTRY='${ECR_REGISTRY}' ECR_REPOSITORY='${ECR_REPOSITORY}' IMAGE_TAG='${IMAGE_TAG}' docker compose -f docker-compose.yml -f loadtest/setup/infra/docker-compose.loadtest.yml --profile prod up -d"

  scp -r "${SSH_OPTIONS[@]}" "${INFRA_DIR}/monitoring" \
    "${EC2_USER}@${OBSERVABILITY_PUBLIC_IP}:${REMOTE_OBSERVABILITY_ROOT}/"
  ssh "${SSH_OPTIONS_NO_STDIN[@]}" "${EC2_USER}@${OBSERVABILITY_PUBLIC_IP}" \
    "sudo chmod -R a+rX '${REMOTE_OBSERVABILITY_ROOT}/monitoring'"
  ssh "${SSH_OPTIONS_NO_STDIN[@]}" "${EC2_USER}@${OBSERVABILITY_PUBLIC_IP}" \
    "sudo sh -c \"cd '${REMOTE_OBSERVABILITY_ROOT}/monitoring' && export GRAFANA_ADMIN_PASSWORD='${GRAFANA_ADMIN_PASSWORD:-imhere-test-grafana-password}' GRAFANA_ROOT_URL='http://${OBSERVABILITY_PUBLIC_IP}:3000' && docker compose up -d\""
}

print_summary() {
  cat <<SUMMARY
부하 테스트 환경 구성이 완료되었습니다.
App: ${APP_PUBLIC_IP}
Database private IP: ${DB_PRIVATE_IP}
Observability: ${OBSERVABILITY_PUBLIC_IP}
Image: ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}
SUMMARY
}

main() {
  require_commands
  validate_files
  create_key_pair
  resolve_network_settings
  resolve_runtime_settings
  deploy_stack
  read_stack_outputs
  configure_mysql
  deploy_application
  print_summary
}

main "$@"
