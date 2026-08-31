#!/usr/bin/env bash

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

lineage_is_valid() {
  has_renewal_conf || return 1
  local status
  status=$(sudo certbot certificates --cert-name "$CERT_DOMAIN" 2>&1) || return 1
  if echo "$status" | grep -qE "does not match|INVALID|No certificates found"; then
    return 1
  fi
  return 0
}

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

remove_bootstrap_cert() {
  if has_renewal_conf; then
    echo "$CERT_LIVE_DIR를 지우지 않습니다: renewal 메타데이터가 존재합니다." >&2
    return 1
  fi
  if sudo test -d "/etc/letsencrypt/archive/$CERT_DOMAIN" \
    && [[ -n "$(sudo ls -A "/etc/letsencrypt/archive/$CERT_DOMAIN" 2>/dev/null)" ]]; then
    echo "$CERT_LIVE_DIR를 지우지 않습니다: archive 디렉터리가 비어 있지 않습니다." >&2
    return 1
  fi
  if is_letsencrypt_cert || ! is_bootstrap_cert; then
    echo "$CERT_LIVE_DIR를 지우지 않습니다: bootstrap 인증서가 아닙니다." >&2
    return 1
  fi

  echo "certbot이 정식 인증서를 만들 수 있도록 bootstrap 인증서를 제거합니다."
  sudo rm -rf "$CERT_LIVE_DIR"
  sudo rm -rf "/etc/letsencrypt/archive/$CERT_DOMAIN"
}

purge_unusable_renewal_conf() {
  sudo test -e "$CERT_RENEWAL_CONF" || return 0
  lineage_is_registered && return 0

  if ! renewal_conf_is_missing_required_reference; then
    echo "$CERT_RENEWAL_CONF를 지우지 않습니다: certbot의 다른 오류입니다." >&2
    return 1
  fi

  if sudo test -d "/etc/letsencrypt/archive/$CERT_DOMAIN" \
    && [[ -n "$(sudo ls -A "/etc/letsencrypt/archive/$CERT_DOMAIN" 2>/dev/null)" ]]; then
    echo "$CERT_RENEWAL_CONF를 지우지 않습니다: archive 디렉터리에 인증서가 있습니다." >&2
    return 1
  fi

  echo "사용할 수 없는 renewal 설정 $CERT_RENEWAL_CONF를 제거합니다."
  sudo rm -f "$CERT_RENEWAL_CONF"
}

reclaim_suffixed_lineage() {
  lineage_is_registered && return 0
  sudo test -d /etc/letsencrypt/renewal || return 0

  local stale name
  stale=$(sudo find /etc/letsencrypt/renewal -maxdepth 1 \
    -name "${CERT_DOMAIN}-[0-9][0-9][0-9][0-9].conf" -printf '%f\n' 2>/dev/null \
    | sed 's/\.conf$//' | sort)
  [[ -n "$stale" ]] || return 0

  for name in $stale; do
    echo "$CERT_DOMAIN의 정식 lineage를 위해 certbot lineage $name을 삭제합니다."
    sudo certbot delete --cert-name "$name" --non-interactive
  done
}

verify_canonical_lineage() {
  has_live_cert && return 0

  echo "발급은 성공했지만 $CERT_LIVE_DIR/fullchain.pem이 없습니다." >&2
  echo "nginx가 참조하는 인증서 경로가 없으므로 배포를 중단합니다." >&2
  sudo certbot certificates || true
  return 1
}
