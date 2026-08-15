#!/usr/bin/env bash
#
# 배포 후 헬스체크. `docker compose up -d`는 컨테이너 생성만 보장하므로
# 기동 직후 죽는 경우(누락된 env, ddl-auto=validate 스키마 불일치 등)를
# CD가 성공으로 오보한다. 이 스크립트가 그 구멍을 막는다.
#
# 앱 이미지에는 curl이 없다. 같은 compose 네트워크에 있는 nginx 컨테이너의
# busybox wget으로 관리 포트(4861)를 직접 찌른다.
#
# 필요한 환경변수
#   EC2_DEPLOY_PATH   배포 루트. env/*.env를 여기서 읽는다.
#   MGMT_BASE_PATH    env/app.env에서 읽는다 (난독화된 actuator base-path).
#   HEALTH_ATTEMPTS   선택. 기본 10회.
#   HEALTH_INTERVAL   선택. 기본 30초.

set -euo pipefail

: "${EC2_DEPLOY_PATH:?EC2_DEPLOY_PATH is required}"

set -a
for env_file in "${EC2_DEPLOY_PATH}"/env/*.env; do
  # shellcheck disable=SC1090
  . "$env_file"
done
set +a

: "${MGMT_BASE_PATH:?MGMT_BASE_PATH must be set in env/app.env}"

ATTEMPTS="${HEALTH_ATTEMPTS:-10}"
INTERVAL="${HEALTH_INTERVAL:-30}"
HEALTH_URL="http://iamhere-server-container:4861${MGMT_BASE_PATH}/health"

healthy=false
for attempt in $(seq 1 "$ATTEMPTS"); do
  if body=$(sudo docker exec nginx-container wget -q -T 10 -O - "$HEALTH_URL" 2>/dev/null); then
    echo "Attempt ${attempt}/${ATTEMPTS}: ${body}"
    case "$body" in
      *'"status":"UP"'*)
        healthy=true
        break
        ;;
    esac
  else
    echo "Attempt ${attempt}/${ATTEMPTS}: no response yet."
  fi

  if [ "$attempt" -lt "$ATTEMPTS" ]; then
    sleep "$INTERVAL"
  fi
done

if [ "$healthy" != "true" ]; then
  echo "App did not become healthy within $((ATTEMPTS * INTERVAL))s. Recent app logs:"
  sudo docker logs --tail 200 iamhere-server-container || true
  exit 1
fi

echo "App is healthy."
