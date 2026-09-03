#!/usr/bin/env bash

set -euo pipefail

require_environment() {
  : "${EC2_DEPLOY_PATH:?EC2_DEPLOY_PATH 환경변수가 필요합니다.}"
  : "${ECR_REGISTRY:?ECR_REGISTRY 환경변수가 필요합니다.}"
  : "${ECR_REPOSITORY:?ECR_REPOSITORY 환경변수가 필요합니다.}"
}

normalize_deploy_path() {
  case "$EC2_DEPLOY_PATH" in
    "~") EC2_DEPLOY_PATH="$HOME" ;;
    "~/"*) EC2_DEPLOY_PATH="$HOME/${EC2_DEPLOY_PATH#~/}" ;;
  esac

  cd -- "$EC2_DEPLOY_PATH"
  EC2_DEPLOY_PATH="$PWD"
}

login_to_ecr() {
  sudo docker login --username AWS --password-stdin "$ECR_REGISTRY"
}

compose() {
  sudo env ECR_REGISTRY="$ECR_REGISTRY" ECR_REPOSITORY="$ECR_REPOSITORY" \
    docker compose -f docker-compose.yml --profile prod "$@"
}

validate_compose_config() {
  compose config >/dev/null
}

validate_nginx_config() {
  sudo docker run --rm \
    --add-host iamhere-server-container:127.0.0.1 \
    -v "$PWD/infra/nginx/nginx.conf:/etc/nginx/nginx.conf:ro" \
    -v /etc/letsencrypt:/etc/letsencrypt:ro \
    nginx:alpine nginx -t
}

pull_images() {
  compose pull
}

start_services() {
  compose up -d
}

recreate_alloy() {
  # alloy-config.alloy는 바인드 마운트 파일이므로 파일 교체만으로는
  # 실행 중인 Alloy 프로세스가 새 설정을 읽는다고 보장할 수 없다.
  compose up -d --force-recreate alloy
}

main() {
  require_environment
  normalize_deploy_path
  login_to_ecr
  validate_compose_config
  validate_nginx_config
  pull_images
  start_services
  recreate_alloy

  echo "컨테이너 롤아웃이 완료되었습니다."
}

main "$@"
