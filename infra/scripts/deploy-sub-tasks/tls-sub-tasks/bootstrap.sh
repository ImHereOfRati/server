#!/usr/bin/env bash

generate_bootstrap_cert() {
  sudo mkdir -p "$CERT_LIVE_DIR"
  sudo sh -c "printf '%s\\n' \
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
    -keyout "$CERT_LIVE_DIR/privkey.pem" \
    -out "$CERT_LIVE_DIR/fullchain.pem" \
    -config /tmp/imhere-cert.cnf \
    -extensions v3_req
  sudo rm -f /tmp/imhere-cert.cnf
}

ensure_ssl_options() {
  if sudo test -s "$SSL_OPTIONS_FILE" && sudo grep -q '^ssl_ciphers' "$SSL_OPTIONS_FILE"; then
    echo "기존 $SSL_OPTIONS_FILE을 사용합니다."
    return 0
  fi

  echo "$SSL_OPTIONS_FILE을 작성합니다."
  sudo tee "$SSL_OPTIONS_FILE" >/dev/null <<'EOF'
# Managed by infra/scripts/deploy-sub-tasks/tls-sub-tasks/bootstrap.sh.
# Contents are based on the Mozilla intermediate profile.
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
  if sudo test -s "$DHPARAMS_FILE" && sudo openssl dhparam -in "$DHPARAMS_FILE" -noout >/dev/null 2>&1; then
    echo "기존 $DHPARAMS_FILE을 사용합니다."
    return 0
  fi

  echo "$DHPARAMS_FILE을 작성합니다."
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

bootstrap_tls() {
  sudo mkdir -p /etc/letsencrypt
  ensure_ssl_options
  ensure_dhparams

  if ! has_live_cert && ! has_renewal_conf; then
    echo "$CERT_DOMAIN 인증서가 없습니다. bootstrap self-signed 인증서를 생성합니다."
    generate_bootstrap_cert
  else
    echo "$CERT_DOMAIN 인증서 관련 파일이 이미 있습니다. 기존 파일을 유지합니다."
  fi
}
