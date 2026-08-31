#!/usr/bin/env bash

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

initialize_tls_context() {
  : "${EC2_DEPLOY_PATH:?EC2_DEPLOY_PATH 환경변수가 필요합니다.}"

  case "$EC2_DEPLOY_PATH" in
    "~") EC2_DEPLOY_PATH="$HOME" ;;
    "~/"*) EC2_DEPLOY_PATH="$HOME/${EC2_DEPLOY_PATH#~/}" ;;
  esac

  NGINX_ENV="${EC2_DEPLOY_PATH}/env/nginx.env"
  [[ -f "$NGINX_ENV" ]] || {
    echo "dotenv 파일이 없습니다: $NGINX_ENV" >&2
    return 1
  }

  CERT_DOMAIN="$(read_dotenv_value CERT_DOMAIN "$NGINX_ENV")"
  : "${CERT_DOMAIN:?env/nginx.env에 CERT_DOMAIN이 설정되어 있어야 합니다.}"

  CERT_ALT_DOMAIN="www.${CERT_DOMAIN}"
  CERT_LIVE_DIR="/etc/letsencrypt/live/${CERT_DOMAIN}"
  CERT_RENEWAL_CONF="/etc/letsencrypt/renewal/${CERT_DOMAIN}.conf"
  WEBROOT="${EC2_DEPLOY_PATH}/infra/nginx/certbot"
  SSL_OPTIONS_FILE="/etc/letsencrypt/options-ssl-nginx.conf"
  DHPARAMS_FILE="/etc/letsencrypt/ssl-dhparams.pem"
}
