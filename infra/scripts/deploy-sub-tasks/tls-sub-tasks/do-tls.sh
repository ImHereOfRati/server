#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TASK_DIR="$SCRIPT_DIR"

source "$TASK_DIR/common.sh"
source "$TASK_DIR/certificate-state.sh"
source "$TASK_DIR/bootstrap.sh"
source "$TASK_DIR/issue.sh"

usage() {
  cat <<'EOF'
사용법: do-tls.sh {bootstrap|issue}
EOF
}

main() {
  local mode="${1:-}"

  case "$mode" in
    bootstrap|issue) ;;
    *)
      usage >&2
      return 2
      ;;
  esac

  initialize_tls_context

  case "$mode" in
    bootstrap) bootstrap_tls ;;
    issue) issue_tls ;;
  esac
}

main "$@"
