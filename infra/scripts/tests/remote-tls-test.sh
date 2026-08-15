#!/usr/bin/env bash

set -euo pipefail

mock_sudo() {
  local command="${1:-}"
  shift || true

  case "$command" in
    test)
      case "$1:$2" in
        -e:/etc/letsencrypt/renewal/example.com.conf)
          test -e "$MOCK_STATE/invalid-canonical-conf"
          ;;
        -s:/etc/letsencrypt/renewal/example.com.conf)
          test -e "$MOCK_STATE/canonical-issued"
          ;;
        -d:/etc/letsencrypt/archive/example.com)
          return 1
          ;;
        -d:/etc/letsencrypt/renewal)
          return 0
          ;;
        -f:/etc/letsencrypt/live/example.com/fullchain.pem|-f:/etc/letsencrypt/live/example.com/privkey.pem)
          test -e "$MOCK_STATE/bootstrap" || test -e "$MOCK_STATE/canonical-issued"
          ;;
        *)
          echo "Unexpected mocked test: $*" >&2
          return 1
          ;;
      esac
      ;;
    certbot)
      case "${1:-}" in
        certificates)
          if test -e "$MOCK_STATE/invalid-canonical-conf" && ! test -e "$MOCK_STATE/unknown-diagnosis"; then
            printf 'Renewal configuration file /etc/letsencrypt/renewal/example.com.conf produced an unexpected error: renewal config file {} is missing a required file reference. Skipping.\n' >&2
          fi
          if test -e "$MOCK_STATE/suffixed-lineage"; then
            printf '  Certificate Name: example.com-0001\n'
          elif test -e "$MOCK_STATE/canonical-issued"; then
            printf '  Certificate Name: example.com\n'
          fi
          ;;
        delete)
          printf '%s\n' 'delete-suffixed-lineage' >> "$MOCK_LOG"
          rm -f "$MOCK_STATE/suffixed-lineage"
          ;;
        certonly)
          printf '%s\n' 'issue-canonical-lineage' >> "$MOCK_LOG"
          touch "$MOCK_STATE/canonical-issued"
          ;;
        *)
          echo "Unexpected certbot command: $*" >&2
          return 1
          ;;
      esac
      ;;
    find)
      if test -e "$MOCK_STATE/suffixed-lineage"; then
        printf 'example.com-0001.conf\n'
      fi
      ;;
    openssl)
      printf 'issuer=CN=example.com\n'
      ;;
    rm)
      case "$*" in
        '-f /etc/letsencrypt/renewal/example.com.conf')
          printf '%s\n' 'remove-invalid-canonical-conf' >> "$MOCK_LOG"
          rm -f "$MOCK_STATE/invalid-canonical-conf"
          ;;
        '-rf /etc/letsencrypt/live/example.com')
          printf '%s\n' 'remove-bootstrap-certificate' >> "$MOCK_LOG"
          rm -f "$MOCK_STATE/bootstrap"
          ;;
        '-rf /etc/letsencrypt/archive/example.com')
          printf '%s\n' 'remove-empty-canonical-archive' >> "$MOCK_LOG"
          ;;
        *)
          echo "Unexpected mocked rm: $*" >&2
          return 1
          ;;
      esac
      ;;
    docker)
      printf '%s\n' 'reload-nginx' >> "$MOCK_LOG"
      ;;
    *)
      echo "Unexpected sudo command: $command $*" >&2
      return 1
      ;;
  esac
}

if [[ "${0##*/}" == sudo ]]; then
  mock_sudo "$@"
  exit
fi

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
REPOSITORY_ROOT=$(cd -- "$SCRIPT_DIR/../../.." && pwd)
TEST_TMP=$(mktemp -d)
trap 'test -n "${TEST_TMP:-}" && rm -rf -- "${TEST_TMP:?}"' EXIT

export MOCK_STATE="$TEST_TMP/state"
export MOCK_LOG="$TEST_TMP/operations.log"
mkdir -p "$MOCK_STATE" "$TEST_TMP/bin" "$TEST_TMP/deploy/env"
touch "$MOCK_STATE/bootstrap" "$MOCK_STATE/invalid-canonical-conf" "$MOCK_STATE/suffixed-lineage"
cp "$SCRIPT_DIR/remote-tls-test.sh" "$TEST_TMP/bin/sudo"
chmod +x "$TEST_TMP/bin/sudo"
printf 'CERT_DOMAIN=example.com\n' > "$TEST_TMP/deploy/env/web.env"

PATH="$TEST_TMP/bin:$PATH" \
  EC2_DEPLOY_PATH="$TEST_TMP/deploy" \
  bash "$REPOSITORY_ROOT/infra/scripts/remote-tls.sh" issue

expected=$(printf '%s\n' \
  remove-invalid-canonical-conf \
  delete-suffixed-lineage \
  remove-bootstrap-certificate \
  remove-empty-canonical-archive \
  issue-canonical-lineage \
  reload-nginx)
actual=$(cat "$MOCK_LOG")

if [[ "$actual" != "$expected" ]]; then
  printf 'Unexpected TLS recovery operations.\nExpected:\n%s\nActual:\n%s\n' "$expected" "$actual" >&2
  exit 1
fi

test -e "$MOCK_STATE/canonical-issued"
test ! -e "$MOCK_STATE/invalid-canonical-conf"
test ! -e "$MOCK_STATE/suffixed-lineage"
test ! -e "$MOCK_STATE/bootstrap"

printf '' > "$MOCK_LOG"
rm -f "$MOCK_STATE/canonical-issued"
touch "$MOCK_STATE/bootstrap" "$MOCK_STATE/invalid-canonical-conf" "$MOCK_STATE/suffixed-lineage" "$MOCK_STATE/unknown-diagnosis"

if PATH="$TEST_TMP/bin:$PATH" \
  EC2_DEPLOY_PATH="$TEST_TMP/deploy" \
  bash "$REPOSITORY_ROOT/infra/scripts/remote-tls.sh" issue > "$TEST_TMP/refusal.log" 2>&1; then
  echo 'TLS recovery unexpectedly accepted an unknown certbot failure.' >&2
  exit 1
fi

grep -Fq 'certbot did not report the known missing-reference failure' "$TEST_TMP/refusal.log"
test ! -s "$MOCK_LOG"
test -e "$MOCK_STATE/invalid-canonical-conf"
test -e "$MOCK_STATE/suffixed-lineage"
test -e "$MOCK_STATE/bootstrap"

echo 'remote-tls recovery and refusal regression tests passed.'
