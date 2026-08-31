#!/usr/bin/env bash

set -euo pipefail

ATTEMPTS="${HEALTH_ATTEMPTS:-10}"
INTERVAL="${HEALTH_INTERVAL:-30}"
HEALTH_URL=""
BODY=""
HEALTHY=false

require_environment() {
  : "${EC2_DEPLOY_PATH:?EC2_DEPLOY_PATH 환경변수가 필요합니다.}"
}

normalize_deploy_path() {
  case "$EC2_DEPLOY_PATH" in
    "~") EC2_DEPLOY_PATH="$HOME" ;;
    "~/"*) EC2_DEPLOY_PATH="$HOME/${EC2_DEPLOY_PATH#~/}" ;;
  esac
}

read_dotenv_value() {
  local key="$1"
  local file="$2"

  awk -v key="$key" '
    index($0, key "=") == 1 {
      value = substr($0, length(key) + 2)
      sub(/\r$/, "", value)
      print value
      exit
    }
  ' "$file"
}

load_management_path() {
  local app_env="${EC2_DEPLOY_PATH}/env/server.env"

  [[ -f "$app_env" ]] || {
    echo "dotenv 파일이 없습니다: $app_env" >&2
    return 1
  }

  MGMT_BASE_PATH="$(read_dotenv_value MGMT_BASE_PATH "$app_env")"
  : "${MGMT_BASE_PATH:?env/server.env에 MGMT_BASE_PATH가 설정되어 있어야 합니다.}"
}

configure_health_check() {
  HEALTH_URL="http://iamhere-server-container:4861${MGMT_BASE_PATH}/health"
}

check_health() {
  local attempt="$1"

  if BODY=$(sudo docker exec nginx-container wget -q -T 10 -O - "$HEALTH_URL" 2>/dev/null); then
    echo "시도 ${attempt}/${ATTEMPTS}: ${BODY}"
    [[ "$BODY" == *'"status":"UP"'* ]]
  else
    echo "시도 ${attempt}/${ATTEMPTS}: 아직 응답이 없습니다."
    return 1
  fi
}

wait_for_health() {
  local attempt

  for attempt in $(seq 1 "$ATTEMPTS"); do
    if check_health "$attempt"; then
      HEALTHY=true
      return 0
    fi

    if [[ "$attempt" -lt "$ATTEMPTS" ]]; then
      sleep "$INTERVAL"
    fi
  done

  return 1
}

print_failure_logs() {
  echo "$((ATTEMPTS * INTERVAL))초 안에 앱이 정상 상태가 되지 않았습니다. 최근 앱 로그:" >&2
  sudo docker logs --tail 200 iamhere-server-container || true
}

main() {
  require_environment
  normalize_deploy_path
  load_management_path
  configure_health_check

  if ! wait_for_health; then
    print_failure_logs
    return 1
  fi

  echo "앱이 정상 상태입니다."
}

main "$@"
