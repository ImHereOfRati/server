#!/usr/bin/env bash
#
# EC2에서 실행되는 TLS 인증서 관리 스크립트. 두 단계로 나뉜다.
#
#   bootstrap   nginx가 기동할 수 있도록 최소한 하나의 인증서를 보장한다.
#               컨테이너를 올리기 "전"에 실행한다. TLS 설정이 참조하는 파일이
#               없으면 nginx -t부터 실패하기 때문이다.
#
#   issue       Let's Encrypt 발급/갱신 후 nginx를 reload 한다.
#               컨테이너를 올린 "뒤"에 실행한다. HTTP-01 webroot 검증이라
#               nginx가 /.well-known/acme-challenge/ 를 이미 서빙하고 있어야 한다.
#
# 필요한 환경변수
#   EC2_DEPLOY_PATH  배포 루트. env/*.env와 webroot를 여기서 찾는다.
#   CERT_DOMAIN      env/web.env에서 읽는다.

set -euo pipefail

MODE="${1:-}"

: "${EC2_DEPLOY_PATH:?EC2_DEPLOY_PATH is required}"

case "$EC2_DEPLOY_PATH" in
  "~") EC2_DEPLOY_PATH="$HOME" ;;
  "~/"*) EC2_DEPLOY_PATH="$HOME/${EC2_DEPLOY_PATH#~/}" ;;
esac

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

WEB_ENV="${EC2_DEPLOY_PATH}/env/web.env"
if [ ! -f "$WEB_ENV" ]; then
  echo "Missing dotenv file: $WEB_ENV" >&2
  exit 1
fi
CERT_DOMAIN="$(read_dotenv_value CERT_DOMAIN "$WEB_ENV")"

: "${CERT_DOMAIN:?CERT_DOMAIN must be set in env/web.env}"

CERT_ALT_DOMAIN="www.${CERT_DOMAIN}"
CERT_LIVE_DIR="/etc/letsencrypt/live/${CERT_DOMAIN}"
CERT_RENEWAL_CONF="/etc/letsencrypt/renewal/${CERT_DOMAIN}.conf"
WEBROOT="${EC2_DEPLOY_PATH}/infra/nginx/certbot"
SSL_OPTIONS_FILE="/etc/letsencrypt/options-ssl-nginx.conf"
DHPARAMS_FILE="/etc/letsencrypt/ssl-dhparams.pem"

has_live_cert() {
  sudo test -f "$CERT_LIVE_DIR/fullchain.pem" && sudo test -f "$CERT_LIVE_DIR/privkey.pem"
}

has_renewal_conf() {
  sudo test -s "$CERT_RENEWAL_CONF"
}

cert_issuer() {
  sudo openssl x509 -in "$CERT_LIVE_DIR/fullchain.pem" -noout -issuer 2>/dev/null || true
}

is_letsencrypt_cert() {
  cert_issuer | grep -q "Let's Encrypt"
}

is_bootstrap_cert() {
  cert_issuer | grep -q "CN=$CERT_DOMAIN"
}

generate_bootstrap_cert() {
  # bootstrap self-signed는 certbot lineage가 아니다. 과거 renewal/archive가
  # 남아 있는데 live/에만 self-signed를 덮어쓰면, 다음 배포의 certbot renew가
  # 이 self-signed를 정식 cert로 착각해 desync(fullchain does not match
  # cert + chain) 된다.
  #
  # 예전에는 lineage를 통째로 지워서 그 공존을 막았다. 지금은 지우지 않는다 —
  # 운영 TLS를 자동으로 파괴하는 경로였기 때문이다. 대신 호출부(bootstrap)가
  # "live cert도 renewal conf도 없을 때"만 이 함수를 부르고, issue 쪽은 계보가
  # 수상하면 손대지 않고 배포를 실패시킨다.
  sudo mkdir -p "$CERT_LIVE_DIR"
  sudo sh -c "printf '%s\n' \
    '[req]' \
    'distinguished_name = req_distinguished_name' \
    'x509_extensions = v3_req' \
    'prompt = no' \
    '' \
    '[req_distinguished_name]' \
    'CN = $CERT_DOMAIN' \
    '' \
    '[v3_req]' \
    'subjectAltName = @alt_names' \
    '' \
    '[alt_names]' \
    'DNS.1 = $CERT_DOMAIN' \
    'DNS.2 = $CERT_ALT_DOMAIN' \
    > /tmp/imhere-cert.cnf"
  sudo openssl req -x509 -nodes -newkey rsa:2048 -days 30 \
    -keyout "$CERT_LIVE_DIR"/privkey.pem \
    -out "$CERT_LIVE_DIR"/fullchain.pem \
    -config /tmp/imhere-cert.cnf \
    -extensions v3_req
  sudo rm -f /tmp/imhere-cert.cnf
}

# lineage_is_valid: renewal conf가 있고 certbot이 정상 lineage로 인정할 때만 true.
# test -f 만으로는 fullchain desync(does not match)를 못 잡아 과거 장애가 반복됐다.
lineage_is_valid() {
  has_renewal_conf || return 1
  local status
  status=$(sudo certbot certificates --cert-name "$CERT_DOMAIN" 2>&1) || return 1
  if echo "$status" | grep -qE "does not match|INVALID|No certificates found"; then
    return 1
  fi
  return 0
}

# certbot은 새 lineage를 만들 때 live/<domain> 이 이미 있으면
# "live directory exists for <domain>" 로 거부한다. bootstrap self-signed가
# 바로 그 자리를 차지하고 있으므로, 발급 직전에 치워 줘야 한다.
#
# 지우기 전에 "이건 certbot 계보가 아니다"를 세 가지로 확인한다. renewal conf가
# 없고, archive에 내용이 없고, 발급자가 우리가 만든 self-signed 여야 한다.
# 하나라도 어긋나면 정식 인증서일 수 있으므로 지우지 않고 배포를 실패시킨다.
remove_bootstrap_cert() {
  if has_renewal_conf; then
    echo "Refusing to remove $CERT_LIVE_DIR: renewal metadata exists."
    exit 1
  fi
  if sudo test -d "/etc/letsencrypt/archive/$CERT_DOMAIN" \
    && [ -n "$(sudo ls -A "/etc/letsencrypt/archive/$CERT_DOMAIN" 2>/dev/null)" ]; then
    echo "Refusing to remove $CERT_LIVE_DIR: archive directory is not empty."
    exit 1
  fi
  if is_letsencrypt_cert || ! is_bootstrap_cert; then
    echo "Refusing to remove $CERT_LIVE_DIR: certificate is not the bootstrap self-signed one."
    exit 1
  fi

  echo "Clearing bootstrap self-signed certificate so certbot can create its lineage."
  sudo rm -rf "$CERT_LIVE_DIR"
  # archive/ 도 같이 지운다. 비어 있어도 디렉터리가 남아 있으면 certbot은 그
  # 이름이 이미 쓰인 것으로 보고 조용히 <domain>-0001 계보를 만든다. 발급은
  # 성공하지만 nginx가 보는 live/<domain> 은 여전히 없어서 기동이 깨진다.
  # 위 가드가 비어 있음을 이미 확인했으므로 여기서 지우는 건 안전하다.
  sudo rm -rf "/etc/letsencrypt/archive/$CERT_DOMAIN"
}

# certbot이 유효한 계보로 인정하는지 이름 단위로 묻는다. 목록의 이름 끝을
# 고정해서 <domain>-0001 이 <domain> 으로 잘못 매치되지 않게 한다.
lineage_is_registered() {
  sudo certbot certificates 2>/dev/null \
    | sed -n 's/^[[:space:]]*Certificate Name: //p' \
    | grep -Fxq -- "$CERT_DOMAIN"
}

renewal_conf_is_missing_required_reference() {
  local status
  status=$(sudo certbot certificates --cert-name "$CERT_DOMAIN" 2>&1 || true)
  printf '%s\n' "$status" | grep -Fq 'is missing a required file reference'
}

# 정식 이름 자리에 쓸 수 없는 renewal conf가 남아 있으면 지운다.
#
# certbot은 renewal/<domain>.conf 가 존재하기만 하면 그 이름이 쓰인 것으로 보고
# 말없이 <domain>-0001 로 발급한다. 파일이 깨져 있어도 마찬가지다. 실제로
# "renewal config file is missing a required file reference" 상태의 conf가 남아
# 배포가 반복해서 같은 자리에서 깨졌다.
#
# 지우기 전에 두 가지를 확인한다. certbot이 이 이름을 유효한 계보로 세지 않고,
# archive에 실제 인증서도 없어야 한다. 둘 중 하나라도 아니면 진짜 인증서일 수
# 있으므로 건드리지 않고 배포를 실패시킨다.
purge_unusable_renewal_conf() {
  sudo test -e "$CERT_RENEWAL_CONF" || return 0

  if lineage_is_registered; then
    return 0
  fi

  if ! renewal_conf_is_missing_required_reference; then
    echo "Refusing to remove $CERT_RENEWAL_CONF: certbot did not report the known missing-reference failure."
    exit 1
  fi

  if sudo test -d "/etc/letsencrypt/archive/$CERT_DOMAIN" \
    && [ -n "$(sudo ls -A "/etc/letsencrypt/archive/$CERT_DOMAIN" 2>/dev/null)" ]; then
    echo "Refusing to remove $CERT_RENEWAL_CONF: archive directory holds real certificates."
    exit 1
  fi

  echo "Removing unusable renewal config $CERT_RENEWAL_CONF (certbot does not count it as a lineage)."
  sudo rm -f "$CERT_RENEWAL_CONF"
}

# 과거 배포가 남긴 <domain>-0001 류 계보를 정리한다. 정식 계보가 이미 있으면
# 손대지 않는다 — 그때는 그냥 오래된 잔재라 지울 이유도 없다.
reclaim_suffixed_lineage() {
  # 판단은 certbot에게 맡긴다. live/ 의 파일은 bootstrap이 방금 만든 self-signed일
  # 수 있고, renewal conf는 깨진 채 남아 있을 수 있어 둘 다 근거가 못 된다.
  if lineage_is_registered; then
    return 0
  fi

  # certbot을 한 번도 실행하지 않은 새 호스트에는 renewal 디렉터리 자체가 없을
  # 수 있다. 그 상태는 정리할 접미사 계보가 없다는 뜻이지 오류가 아니다.
  sudo test -d /etc/letsencrypt/renewal || return 0

  local stale
  stale=$(sudo find /etc/letsencrypt/renewal -maxdepth 1 \
    -name "${CERT_DOMAIN}-[0-9][0-9][0-9][0-9].conf" -printf '%f\n' 2>/dev/null \
    | sed 's/\.conf$//' | sort)
  [ -n "$stale" ] || return 0

  # 정식 이름이 비어 있는데 접미사 계보만 있는 상태다. 이름을 바꾸는 명령이
  # certbot에 없으므로 지우고 정식 이름으로 다시 받는다. 발급 횟수를 한 번 더
  # 쓰지만, 이 분기는 정식 계보가 생기는 순간 다시 타지 않는다.
  local name
  for name in $stale; do
    echo "Deleting stray lineage $name so $CERT_DOMAIN can take its canonical name."
    sudo certbot delete --cert-name "$name" --non-interactive
  done
}

# 발급이 성공했다고 nginx가 읽을 수 있는 건 아니다. nginx.conf는
# live/<CERT_DOMAIN> 을 하드코딩하므로 certbot이 다른 이름에 넣었으면 여기서
# 잡아야 한다. 놓치면 컨테이너가 기동 단계에서 죽는다.
verify_canonical_lineage() {
  if has_live_cert; then
    return 0
  fi

  echo "Issuance reported success but $CERT_LIVE_DIR/fullchain.pem does not exist."
  echo "nginx reads that exact path, so the deploy cannot continue. Current lineages:"
  sudo certbot certificates || true
  exit 1
}

issue_certificate() {
  sudo certbot certonly --webroot \
    -w "$WEBROOT" \
    --non-interactive \
    --agree-tos \
    --register-unsafely-without-email \
    --keep-until-expiring \
    --cert-name "$CERT_DOMAIN" \
    -d "$CERT_DOMAIN" \
    -d "$CERT_ALT_DOMAIN"
}

# nginx.conf.template이 include 하는 두 파일을 보장한다.
#
# 예전에는 certbot 깃허브 raw URL에서 받아왔는데, upstream이 src/ 레이아웃으로
# 옮기면서 두 경로가 모두 404가 됐고 배포가 통째로 멈췄다. 내용 자체는 공개
# 표준이고 크기도 작으므로, 남의 브랜치 구조에 의존하지 않도록 스크립트에
# 인라인한다.
ensure_ssl_options() {
  # 이미 쓸 만한 파일이 있으면 건드리지 않는다. ssl_ciphers 유무로 판별한다 —
  # 크기 0이거나 과거 curl이 반 쯤 받다 만 파일을 그대로 두면 nginx -t가 깨진다.
  if sudo test -s "$SSL_OPTIONS_FILE" && sudo grep -q '^ssl_ciphers' "$SSL_OPTIONS_FILE"; then
    echo "Reusing existing $SSL_OPTIONS_FILE."
    return 0
  fi

  echo "Writing $SSL_OPTIONS_FILE (Mozilla intermediate profile)."
  # https://ssl-config.mozilla.org 의 intermediate 프로파일. certbot이 배포하는
  # options-ssl-nginx.conf 와 동일한 내용이다.
  sudo tee "$SSL_OPTIONS_FILE" >/dev/null <<'EOF'
# Managed by infra/scripts/remote-tls.sh. Contents are based on
# https://ssl-config.mozilla.org (intermediate profile) and match the
# options-ssl-nginx.conf that certbot ships with its nginx plugin.

ssl_session_cache shared:le_nginx_SSL:10m;
ssl_session_timeout 1440m;
ssl_session_tickets off;

ssl_protocols TLSv1.2 TLSv1.3;
ssl_prefer_server_ciphers off;

ssl_ciphers "ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384";
EOF
  sudo chmod 644 "$SSL_OPTIONS_FILE"
}

ensure_dhparams() {
  # openssl로 실제 파싱까지 해 본다. 존재 여부만 보면 깨진 파일을 재사용한다.
  if sudo test -s "$DHPARAMS_FILE" && sudo openssl dhparam -in "$DHPARAMS_FILE" -noout >/dev/null 2>&1; then
    echo "Reusing existing $DHPARAMS_FILE."
    return 0
  fi

  echo "Writing $DHPARAMS_FILE (RFC 7919 ffdhe2048 group)."
  # openssl dhparam 으로 즉석 생성하지 않는다. 2048비트 생성은 t3 계열에서
  # 수십 초~수 분까지 튀는데, 이 지점은 컨테이너 기동 전 배포 임계 경로다.
  # 대신 RFC 7919의 ffdhe2048 named group을 그대로 쓴다. certbot의
  # ssl-dhparams.pem 이 담고 있던 값과 동일하므로 기존 동작과 바이트 단위로 같다.
  sudo tee "$DHPARAMS_FILE" >/dev/null <<'EOF'
-----BEGIN DH PARAMETERS-----
MIIBCAKCAQEA//////////+t+FRYortKmq/cViAnPTzx2LnFg84tNpWp4TZBFGQz
+8yTnc4kmz75fS/jY2MMddj2gbICrsRhetPfHtXV/WVhJDP1H18GbtCFY2VVPe0a
87VXE15/V8k1mE8McODmi3fipona8+/och3xWKE2rec1MKzKT0g6eXq8CrGCsyT7
YdEIqUuyyOP7uWrat2DX9GgdT0Kj3jlN9K5W7edjcrsZCwenyO4KbXCeAvzhzffi
7MA0BM0oNC9hkXL+nOmFg/+OTxIy7vKBg8P+OxtMb61zO7X8vC7CIAXFjvGDfRaD
ssbzSibBsu/6iGtCOGEoXJf//////////wIBAg==
-----END DH PARAMETERS-----
EOF
  sudo chmod 644 "$DHPARAMS_FILE"
}

bootstrap() {
  sudo mkdir -p /etc/letsencrypt
  ensure_ssl_options
  ensure_dhparams

  if ! has_live_cert && ! has_renewal_conf; then
    echo "No certificate found for $CERT_DOMAIN. Generating a bootstrap self-signed certificate."
    generate_bootstrap_cert
  else
    echo "Existing certificate material found for $CERT_DOMAIN. Leaving it untouched."
  fi
}

issue() {
  # 순서가 중요하다. 깨진 conf를 먼저 치워야 certbot이 정식 이름을 비어 있는
  # 것으로 보고, 그래야 접미사 계보를 지운 뒤 정식 이름으로 발급할 수 있다.
  purge_unusable_renewal_conf
  reclaim_suffixed_lineage

  if lineage_is_valid; then
    if sudo certbot renew --quiet --webroot -w "$WEBROOT"; then
      echo "Validated existing Let's Encrypt lineage for $CERT_DOMAIN."
    else
      echo "Renew failed on an existing Let's Encrypt lineage. Keeping current certificate and failing deploy."
      exit 1
    fi
  elif has_renewal_conf; then
    echo "Existing Let's Encrypt lineage is corrupted. Refusing automatic cleanup to avoid replacing production TLS with self-signed."
    exit 1
  elif has_live_cert && is_letsencrypt_cert; then
    echo "Let's Encrypt certificate exists without renewal metadata. Refusing automatic mutation."
    exit 1
  elif has_live_cert && is_bootstrap_cert; then
    echo "Bootstrap certificate detected. Attempting first Let's Encrypt issuance."
    remove_bootstrap_cert
    if issue_certificate; then
      echo "Issued Let's Encrypt certificate for $CERT_DOMAIN and $CERT_ALT_DOMAIN."
      verify_canonical_lineage
    else
      echo "Let's Encrypt issuance failed during bootstrap. Restoring bootstrap certificate and failing deploy."
      generate_bootstrap_cert
      exit 1
    fi
  else
    # 여기서 self-signed를 먼저 만들면 안 된다. live/ 를 선점해 certbot이
    # 곧바로 "live directory exists" 로 실패한다. issue는 nginx가 이미 뜬 뒤에
    # 도는 단계라 인증서가 당장 필요하지도 않다. 실패했을 때만 만들어 둔다.
    echo "No existing certificate found. Attempting first Let's Encrypt issuance."
    if issue_certificate; then
      echo "Issued Let's Encrypt certificate for $CERT_DOMAIN and $CERT_ALT_DOMAIN."
      verify_canonical_lineage
    else
      echo "Let's Encrypt issuance failed. Generating a bootstrap certificate so nginx can restart, and failing deploy."
      generate_bootstrap_cert
      exit 1
    fi
  fi

  sudo docker exec nginx-container nginx -s reload
}

case "$MODE" in
  bootstrap) bootstrap ;;
  issue) issue ;;
  *)
    echo "usage: remote-tls.sh {bootstrap|issue}" >&2
    exit 2
    ;;
esac
