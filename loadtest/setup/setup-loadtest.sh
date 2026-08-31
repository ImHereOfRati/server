#!/usr/bin/env bash

# ImHere 부하 테스트 환경 전체 구성 스크립트
#
# 수행 순서:
#   1. CloudFormation으로 VPC, 앱 EC2, DB EC2, 관측성 EC2 생성
#   2. DB EC2에 MySQL 직접 설치 및 초기화
#   3. 앱/관측성 EC2에 설정을 배포하고 서비스를 실행
#
# 실행하지 않는 사전 요구사항:
#   - AWS CLI, SSH, SCP 설치 및 AWS 인증 완료
#   - cd.yml이 push한 ECR 이미지 존재

set -euo pipefail

# Windows Git Bash에서 AWS CLI가 CloudFormation 템플릿의 한글을 cp949로
# 해석하지 않도록 파일 입력 인코딩을 UTF-8로 고정한다.
export AWS_CLI_FILE_ENCODING="${AWS_CLI_FILE_ENCODING:-UTF-8}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

# 배포 대상과 CD 파이프라인의 ECR 이미지 정보를 환경변수로 받는다.
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
STACK_NAME="${STACK_NAME:-imhere-loadtest}"
PROD_STACK_NAME="${PROD_STACK_NAME:-imhere-prod-infra}"
EC2_USER="${EC2_USER:-ec2-user}"
REMOTE_APP_ROOT="${REMOTE_APP_ROOT:-/opt/imhere}"
REMOTE_OBSERVABILITY_ROOT="${REMOTE_OBSERVABILITY_ROOT:-/opt/imhere-observability}"
IMAGE_TAG="${IMAGE_TAG:-latest}"
KEY_DIR="${KEY_DIR:-${SCRIPT_DIR}/info/keys}"
STATE_FILE="${STATE_FILE:-${SCRIPT_DIR}/.loadtest-state}"

command -v aws >/dev/null 2>&1 || { echo "aws CLI is required" >&2; exit 1; }
command -v ssh >/dev/null 2>&1 || { echo "ssh is required" >&2; exit 1; }
command -v scp >/dev/null 2>&1 || { echo "scp is required" >&2; exit 1; }

# 매 실행마다 새 EC2 Key Pair를 만들고, 삭제 스크립트가 찾을 수 있도록
# 이름과 개인키 경로만 로컬 상태 파일에 기록한다.
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

if [[ -z "${LOAD_GENERATOR_CIDR:-}" ]]; then
  PUBLIC_IP="${PUBLIC_IP:-$(curl -fsSL https://checkip.amazonaws.com | tr -d '[:space:]')}"
  LOAD_GENERATOR_CIDR="${PUBLIC_IP}/32"
fi
OBSERVABILITY_ADMIN_CIDR="${OBSERVABILITY_ADMIN_CIDR:-${LOAD_GENERATOR_CIDR}}"

MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-}"
if [[ -z "${MYSQL_ROOT_PASSWORD}" ]]; then
  read -r -s -p 'MySQL root password: ' MYSQL_ROOT_PASSWORD
  printf '\n' >&2
fi
[[ "${#MYSQL_ROOT_PASSWORD}" -ge 8 ]] || {
  echo "MYSQL_ROOT_PASSWORD must be at least 8 characters." >&2
  exit 1
}

if [[ -z "${ECR_REPOSITORY:-}" ]]; then
  ECR_REPOSITORY="$(aws cloudformation describe-stacks \
    --stack-name "${PROD_STACK_NAME}" \
    --region "${AWS_REGION}" \
    --query "Stacks[0].Outputs[?OutputKey=='EcrRepositoryName'].OutputValue | [0]" \
    --output text)"
fi
ECR_REPOSITORY="${ECR_REPOSITORY:?ECR_REPOSITORY could not be resolved}"
AWS_ACCOUNT_ID="${AWS_ACCOUNT_ID:-$(aws sts get-caller-identity --query Account --output text)}"
ECR_REGISTRY="${ECR_REGISTRY:-${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com}"

CFN_TEMPLATE="${REPO_ROOT}/loadtest/setup/infra/cloudformation/load-test-aws-setup.yaml"
MYSQL_SCRIPT="${REPO_ROOT}/loadtest/setup/infra/database/setup-mysql.sh"
TEST_ENV_DIR="${REPO_ROOT}/loadtest/setup/test-env"
INFRA_DIR="${REPO_ROOT}/loadtest/setup/infra"
TEMP_ENV_DIR="$(mktemp -d)"
trap 'rm -rf "${TEMP_ENV_DIR}"' EXIT

[[ -f "${CFN_TEMPLATE}" ]] || { echo "CloudFormation template not found: ${CFN_TEMPLATE}" >&2; exit 1; }
[[ -f "${MYSQL_SCRIPT}" ]] || { echo "MySQL setup script not found: ${MYSQL_SCRIPT}" >&2; exit 1; }
[[ -d "${TEST_ENV_DIR}" ]] || { echo "Test environment directory not found: ${TEST_ENV_DIR}" >&2; exit 1; }

echo '[1/3] Deploying AWS load-test stack'
aws cloudformation deploy \
  --region "${AWS_REGION}" \
  --template-file "${CFN_TEMPLATE}" \
  --stack-name "${STACK_NAME}" \
  --parameter-overrides \
    "KeyName=${KEY_NAME}" \
    "LoadGeneratorCidr=${LOAD_GENERATOR_CIDR}" \
    "ObservabilityAdminCidr=${OBSERVABILITY_ADMIN_CIDR}"

stack_output() {
  aws cloudformation describe-stacks \
    --region "${AWS_REGION}" \
    --stack-name "${STACK_NAME}" \
    --query "Stacks[0].Outputs[?OutputKey=='$1'].OutputValue | [0]" \
    --output text
}

APP_PUBLIC_IP="$(stack_output AppPublicIp)"
DB_PUBLIC_IP="$(stack_output DbPublicIp)"
DB_PRIVATE_IP="$(stack_output DbPrivateIp)"
OBSERVABILITY_PUBLIC_IP="$(stack_output ObservabilityPublicIp)"
OBSERVABILITY_PRIVATE_IP="$(stack_output ObservabilityPrivateIp)"

SSH_OPTIONS=(-i "${SSH_KEY_PATH}" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=10)
wait_for_ssh() {
  local host="$1"
  local attempt
  for attempt in $(seq 1 60); do
    if ssh "${SSH_OPTIONS[@]}" "${EC2_USER}@${host}" 'true' >/dev/null 2>&1; then
      return 0
    fi
    sleep 5
  done
  echo "SSH connection timed out: ${host}" >&2
  exit 1
}

echo '[2/3] Configuring MySQL'
wait_for_ssh "${DB_PUBLIC_IP}"
scp "${SSH_OPTIONS[@]}" "${MYSQL_SCRIPT}" "${EC2_USER}@${DB_PUBLIC_IP}:/tmp/setup-mysql.sh"
ssh "${SSH_OPTIONS[@]}" "${EC2_USER}@${DB_PUBLIC_IP}" \
  'sudo install -m 0755 /tmp/setup-mysql.sh /opt/mysql/setup-mysql.sh'
ssh "${SSH_OPTIONS[@]}" "${EC2_USER}@${DB_PUBLIC_IP}" \
  "sudo env MYSQL_ROOT_PASSWORD='${MYSQL_ROOT_PASSWORD}' /opt/mysql/setup-mysql.sh"

echo '[3/3] Configuring and starting Spring application'
wait_for_ssh "${APP_PUBLIC_IP}"
wait_for_ssh "${OBSERVABILITY_PUBLIC_IP}"

# 테스트 환경의 동적 IP와 DB 접속 정보는 원본 파일을 변경하지 않고
# 실행용 임시 디렉터리에서만 치환한다.
sed "s|^DB_HOST=.*|DB_HOST=${DB_PRIVATE_IP}|; s|^DB_PASSWORD=.*|DB_PASSWORD=${MYSQL_ROOT_PASSWORD}|" \
  "${TEST_ENV_DIR}/app.env" > "${TEMP_ENV_DIR}/app.env"
sed "s|\${OBSERVABILITY_PRIVATE_IP}|${OBSERVABILITY_PRIVATE_IP}|g" \
  "${TEST_ENV_DIR}/observability.env" > "${TEMP_ENV_DIR}/observability.env"
for env_file in external.env oidc.env web.env; do
  cp "${TEST_ENV_DIR}/${env_file}" "${TEMP_ENV_DIR}/${env_file}"
done

# 앱 EC2에는 운영 Compose와 부하 테스트 override, 테스트용 env/secret/nginx를 배치한다.
ssh "${SSH_OPTIONS[@]}" "${EC2_USER}@${APP_PUBLIC_IP}" \
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

# CD와 동일한 ECR 레지스트리에 로그인한다. 비밀번호는 표준 입력으로만 전달한다.
aws ecr get-login-password --region "${AWS_REGION}" \
  | ssh "${SSH_OPTIONS[@]}" "${EC2_USER}@${APP_PUBLIC_IP}" \
    "sudo docker login --username AWS --password-stdin '${ECR_REGISTRY}'"

ssh "${SSH_OPTIONS[@]}" "${EC2_USER}@${APP_PUBLIC_IP}" \
  "cd '${REMOTE_APP_ROOT}' && sudo env ECR_REGISTRY='${ECR_REGISTRY}' ECR_REPOSITORY='${ECR_REPOSITORY}' IMAGE_TAG='${IMAGE_TAG}' docker compose -f docker-compose.yml -f loadtest/setup/infra/docker-compose.loadtest.yml --profile prod up -d"

scp -r "${SSH_OPTIONS[@]}" "${INFRA_DIR}/monitoring" \
  "${EC2_USER}@${OBSERVABILITY_PUBLIC_IP}:${REMOTE_OBSERVABILITY_ROOT}/"
# 관측성 EC2에는 Prometheus, Loki, Tempo, Grafana 구성을 배치하고 실행한다.
ssh "${SSH_OPTIONS[@]}" "${EC2_USER}@${OBSERVABILITY_PUBLIC_IP}" \
  "sudo sh -c \"cd '${REMOTE_OBSERVABILITY_ROOT}/monitoring' && export GRAFANA_ADMIN_PASSWORD='${GRAFANA_ADMIN_PASSWORD:-imhere-test-grafana-password}' GRAFANA_ROOT_URL='http://${OBSERVABILITY_PUBLIC_IP}:3000' && docker compose up -d\""

cat <<SUMMARY
Load-test environment is configured.
App: ${APP_PUBLIC_IP}
Database private IP: ${DB_PRIVATE_IP}
Observability: ${OBSERVABILITY_PUBLIC_IP}
Image: ${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}
SUMMARY
