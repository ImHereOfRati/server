#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage: sync-config.sh [--output-dir DIR] [--repo-url URL] [--branch BRANCH]

Copies prod.env and imhereFirebaseKey.json from the private config repo into the
given output directory. Defaults to the current GitHub repo contract.
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
cleanup() {
  rm -rf "$temp_dir"
}
trap cleanup EXIT

git clone --depth 1 --branch "$branch" "$repo_url" "$temp_dir"

if [[ ! -f "$temp_dir/prod.env" ]]; then
  echo "prod.env not found in config repo" >&2
  exit 1
fi

if [[ ! -f "$temp_dir/imhereFirebaseKey.json" ]]; then
  echo "imhereFirebaseKey.json not found in config repo" >&2
  exit 1
fi

mkdir -p "$output_dir/secrets"
cp "$temp_dir/prod.env" "$output_dir/prod.env"
cp "$temp_dir/imhereFirebaseKey.json" "$output_dir/secrets/imhereFirebaseKey.json"
