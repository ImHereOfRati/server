#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
사용법: pull-config.sh [--output-dir 디렉터리] [--repo-url URL] [--branch 브랜치]

config 저장소의 env/*.env와 imhereFirebaseKey.json을 지정한 출력
디렉터리로 가져옵니다.

config 저장소 구조:

  env/server.env
  env/nginx.env
  env/alloy.env
  imhereFirebaseKey.json
EOF
}

OUTPUT_DIR=""
REPO_URL="${CONFIG_REPO_URL:-}"
BRANCH="${CONFIG_REPO_BRANCH:-main}"
TEMP_DIR=""
CONFIG_DIR=""
ENV_FILES=()

die() {
  echo "오류: $*" >&2
  exit 1
}

parse_args() {
  while [[ $# -gt 0 ]]; do
    case "$1" in
      --output-dir)
        [[ $# -ge 2 && -n "$2" ]] || die "--output-dir 옵션에 값이 필요합니다"
        OUTPUT_DIR="$2"
        shift 2
        ;;
      --repo-url)
        [[ $# -ge 2 && -n "$2" ]] || die "--repo-url 옵션에 값이 필요합니다"
        REPO_URL="$2"
        shift 2
        ;;
      --branch)
        [[ $# -ge 2 && -n "$2" ]] || die "--branch 옵션에 값이 필요합니다"
        BRANCH="$2"
        shift 2
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        usage >&2
        die "알 수 없는 인자입니다: $1"
        ;;
    esac
  done
}

validate_options() {
  [[ -n "$OUTPUT_DIR" ]] || die "--output-dir 옵션은 필수입니다"

  if [[ -z "$REPO_URL" ]]; then
    : "${CONFIG_REPO_PAT:?CONFIG_REPO_URL이 설정되지 않았습니다. CONFIG_REPO_PAT이 필요합니다}"
    REPO_URL="https://x-access-token:${CONFIG_REPO_PAT}@github.com/ImHereOfRati/config.git"
  fi
}

setup_temp_dir() {
  TEMP_DIR="$(mktemp -d)"
  CONFIG_DIR="$TEMP_DIR/config"
  trap cleanup EXIT
}

cleanup() {
  if [[ -n "$TEMP_DIR" && -d "$TEMP_DIR" ]]; then
    rm -rf "$TEMP_DIR"
  fi
}

clone_config_repo() {
  git clone --depth 1 --branch "$BRANCH" "$REPO_URL" "$CONFIG_DIR"
}

find_env_files() {
  local file

  shopt -s nullglob
  ENV_FILES=("$CONFIG_DIR"/env/*.env)
  shopt -u nullglob

  [[ ${#ENV_FILES[@]} -gt 0 ]] || {
    echo "config 저장소에서 env/*.env 파일을 찾지 못했습니다." >&2
    echo "저장소에는 env/*.env 파일과 imhereFirebaseKey.json이 있어야 합니다." >&2
    return 1
  }

  for file in "${ENV_FILES[@]}"; do
    [[ -f "$file" ]] || return 1
  done
}

validate_config_files() {
  [[ -f "$CONFIG_DIR/imhereFirebaseKey.json" ]] || {
    echo "config 저장소에 imhereFirebaseKey.json이 없습니다." >&2
    return 1
  }
}

prepare_output_directories() {
  mkdir -p "$OUTPUT_DIR/env" "$OUTPUT_DIR/secrets"
}

copy_env_files() {
  cp "${ENV_FILES[@]}" "$OUTPUT_DIR/env/"
}

copy_firebase_key() {
  cp "$CONFIG_DIR/imhereFirebaseKey.json" \
    "$OUTPUT_DIR/secrets/imhereFirebaseKey.json"
}

install_config() {
  prepare_output_directories
  copy_env_files
  copy_firebase_key
}

print_summary() {
  local file

  echo "env 파일 ${#ENV_FILES[@]}개를 가져왔습니다."
  for file in "${ENV_FILES[@]}"; do
    echo "  env/$(basename "$file")"
  done
  echo "  secrets/imhereFirebaseKey.json"
}

main() {
  parse_args "$@"
  validate_options
  setup_temp_dir
  clone_config_repo
  find_env_files
  validate_config_files
  install_config
  print_summary
}

main "$@"
