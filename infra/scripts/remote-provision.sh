#!/usr/bin/env bash
#
# EC2 호스트 준비. 배포에 필요한 패키지(docker, certbot, compose 플러그인)를
# 없을 때만 설치하고 런타임 디렉터리를 만든다.
#
# 이 스크립트는 배포 파일이 호스트에 올라가기 전에 실행되므로 stdin으로 흘려보낸다:
#   ssh host "EC2_DEPLOY_PATH=... bash -s" < infra/scripts/remote-provision.sh
#
# 필요한 환경변수
#   EC2_DEPLOY_PATH  배포 루트 (예: /home/ec2-user/imhere)

set -euo pipefail

: "${EC2_DEPLOY_PATH:?EC2_DEPLOY_PATH is required}"

# Environment variables do not expand `~` themselves. Normalize it before
# creating directories so the provision and deploy steps use the same path.
case "$EC2_DEPLOY_PATH" in
  "~") EC2_DEPLOY_PATH="$HOME" ;;
  "~/"*) EC2_DEPLOY_PATH="$HOME/${EC2_DEPLOY_PATH#~/}" ;;
esac

if ! command -v docker >/dev/null 2>&1; then
  sudo dnf install -y docker
fi

if ! command -v certbot >/dev/null 2>&1; then
  sudo dnf install -y certbot
fi

if ! sudo docker compose version >/dev/null 2>&1; then
  sudo mkdir -p /usr/local/lib/docker/cli-plugins
  sudo curl -fsSL \
    https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
    -o /usr/local/lib/docker/cli-plugins/docker-compose
  sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
fi

sudo systemctl enable --now docker

sudo docker --version
sudo docker compose version
certbot --version

# 배포 루트 자체는 CloudFormation UserData가 root 권한으로 미리 만들어 둔다.
# 그래서 sudo로 만들고 소유권을 배포 계정에 넘긴 뒤에야 이후 scp가 통한다.
# chown은 매번 돌려도 안전하고, 루트가 이미 배포 계정 소유면 아무것도 하지 않는다.
sudo mkdir -p \
  "${EC2_DEPLOY_PATH}/infra/nginx/certbot" \
  "${EC2_DEPLOY_PATH}/infra/alloy" \
  "${EC2_DEPLOY_PATH}/infra/scripts" \
  "${EC2_DEPLOY_PATH}/env" \
  "${EC2_DEPLOY_PATH}/secrets"

sudo chown -R "$(id -u):$(id -g)" "${EC2_DEPLOY_PATH}"

echo "Host provisioned. Deploy root: ${EC2_DEPLOY_PATH}"
