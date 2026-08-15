#!/usr/bin/env bash
#
# EC2에서 실행되는 컨테이너 롤아웃 스크립트.
# compose 정의와 렌더링된 nginx.conf를 먼저 검증한 뒤 이미지를 받아 올린다.
#
# ECR 비밀번호는 stdin으로 받는다. 환경변수나 커맨드라인에 실으면 같은 호스트의
# 다른 프로세스가 /proc에서 들여다볼 수 있다:
#   aws ecr get-login-password | ssh host "... bash remote-rollout.sh"
#
# 필요한 환경변수
#   EC2_DEPLOY_PATH  배포 루트
#   ECR_REGISTRY     <account>.dkr.ecr.<region>.amazonaws.com
#   ECR_REPOSITORY   ECR 리포지토리 이름

set -euo pipefail

: "${EC2_DEPLOY_PATH:?EC2_DEPLOY_PATH is required}"
: "${ECR_REGISTRY:?ECR_REGISTRY is required}"
: "${ECR_REPOSITORY:?ECR_REPOSITORY is required}"

# Environment variables do not expand `~` themselves. Resolve it before
# changing directory; otherwise `EC2_DEPLOY_PATH=~/imhere` is treated as a
# literal directory name by bash.
case "$EC2_DEPLOY_PATH" in
  "~") EC2_DEPLOY_PATH="$HOME" ;;
  "~/"*) EC2_DEPLOY_PATH="$HOME/${EC2_DEPLOY_PATH#~/}" ;;
esac

cd -- "$EC2_DEPLOY_PATH"
EC2_DEPLOY_PATH="$PWD"

sudo docker login --username AWS --password-stdin "$ECR_REGISTRY"

compose() {
  sudo env ECR_REGISTRY="$ECR_REGISTRY" ECR_REPOSITORY="$ECR_REPOSITORY" \
    docker compose -f docker-compose.yml --profile prod "$@"
}

# 1) compose 정의가 해석되는지 (누락된 env_file, 잘못된 보간 등)
compose config >/dev/null

# 2) 렌더링된 nginx.conf가 실제 nginx로 파싱되는지.
#    업스트림 이름은 아직 없으므로 루프백으로 매핑해 이름 해석만 통과시킨다.
sudo docker run --rm \
  --add-host iamhere-server-container:127.0.0.1 \
  -v "$PWD/infra/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" \
  -v /etc/letsencrypt:/etc/letsencrypt:ro \
  nginx:alpine nginx -t

compose pull
compose up -d

echo "Containers rolled out."
