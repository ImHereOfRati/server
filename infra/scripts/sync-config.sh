#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage: sync-config.sh [--output-dir DIR] [--repo-url URL] [--branch BRANCH]

Copies env/*.env and imhereFirebaseKey.json from the private config repo into the
given output directory.

Expected config repo layout:

  env/app.env
  env/web.env
  env/oidc.env
  env/external.env
  env/observability.env
  imhereFirebaseKey.json
EOF
}

output_dir=""
repo_url="${CONFIG_REPO_URL:-}"
branch="${CONFIG_REPO_BRANCH:-main}"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --output-dir)
      output_dir="${2:-}"
      shift 2
      ;;
    --repo-url)
      repo_url="${2:-}"
      shift 2
      ;;
    --branch)
      branch="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown argument: $1" >&2
      usage >&2
      exit 1
      ;;
  esac
done

if [[ -z "$output_dir" ]]; then
  echo "--output-dir is required" >&2
  exit 1
fi

if [[ -z "$repo_url" ]]; then
  : "${CONFIG_REPO_PAT:?CONFIG_REPO_PAT is required when CONFIG_REPO_URL is not set}"
  repo_url="https://x-access-token:${CONFIG_REPO_PAT}@github.com/ImHereOfRati/config.git"
fi

temp_dir="$(mktemp -d)"
config_dir="$temp_dir/config"

cleanup() {
  rm -rf "$temp_dir"
}
trap cleanup EXIT

git clone --depth 1 --branch "$branch" "$repo_url" "$config_dir"

# 런타임 env는 관심사별로 쪼개져 env/ 아래에 있다. 파일 이름을 하드코딩하지 않고
# 있는 것을 전부 가져온다 — config repo에 파일이 하나 늘어도 여기를 고치지 않게.
shopt -s nullglob
env_files=("$config_dir"/env/*.env)
shopt -u nullglob

if [[ ${#env_files[@]} -eq 0 ]]; then
  echo "No env/*.env files found in config repo." >&2
  echo "Expected layout is env/*.env plus imhereFirebaseKey.json in the config repo." >&2
  exit 1
fi

if [[ ! -f "$config_dir/imhereFirebaseKey.json" ]]; then
  echo "imhereFirebaseKey.json not found in config repo" >&2
  exit 1
fi

mkdir -p "$output_dir/env" "$output_dir/secrets"
cp "${env_files[@]}" "$output_dir/env/"
cp "$config_dir/imhereFirebaseKey.json" "$output_dir/secrets/imhereFirebaseKey.json"

echo "Synced ${#env_files[@]} env file(s):"
for file in "${env_files[@]}"; do
  echo "  env/$(basename "$file")"
done
