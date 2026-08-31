#!/usr/bin/env bash

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

issue_tls() {
  purge_unusable_renewal_conf
  reclaim_suffixed_lineage

  if lineage_is_valid; then
    if sudo certbot renew --quiet --webroot -w "$WEBROOT"; then
      echo "$CERT_DOMAIN의 기존 Let's Encrypt 인증서를 확인했습니다."
    else
      echo "기존 Let's Encrypt 인증서 갱신에 실패했습니다." >&2
      return 1
    fi
  elif has_renewal_conf; then
    echo "기존 Let's Encrypt renewal 설정이 손상되어 자동 변경을 중단합니다." >&2
    return 1
  elif has_live_cert && is_letsencrypt_cert; then
    echo "renewal 메타데이터 없이 Let's Encrypt 인증서만 존재하여 자동 변경을 중단합니다." >&2
    return 1
  elif has_live_cert && is_bootstrap_cert; then
    echo "bootstrap 인증서를 감지했습니다. 첫 Let's Encrypt 발급을 시도합니다."
    remove_bootstrap_cert
    if issue_certificate; then
      echo "$CERT_DOMAIN 및 $CERT_ALT_DOMAIN 인증서를 발급했습니다."
      verify_canonical_lineage
    else
      echo "bootstrap 단계의 Let's Encrypt 발급에 실패했습니다. bootstrap 인증서를 복구합니다." >&2
      generate_bootstrap_cert
      return 1
    fi
  else
    echo "기존 인증서가 없습니다. 첫 Let's Encrypt 발급을 시도합니다."
    if issue_certificate; then
      echo "$CERT_DOMAIN 및 $CERT_ALT_DOMAIN 인증서를 발급했습니다."
      verify_canonical_lineage
    else
      echo "Let's Encrypt 발급에 실패했습니다. bootstrap 인증서를 생성합니다." >&2
      generate_bootstrap_cert
      return 1
    fi
  fi

  sudo docker exec nginx-container nginx -s reload
}
