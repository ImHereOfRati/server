#!/usr/bin/env bash

set -Eeuo pipefail

usage() {
  echo "Usage: $0 --db-public-ip IP --ssh-key PATH --password PASSWORD --output REPORT.md" >&2
  exit 2
}

DB_PUBLIC_IP=""
SSH_KEY=""
MYSQL_PASSWORD=""
OUTPUT=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --db-public-ip) DB_PUBLIC_IP="$2"; shift 2 ;;
    --ssh-key) SSH_KEY="$2"; shift 2 ;;
    --password) MYSQL_PASSWORD="$2"; shift 2 ;;
    --output) OUTPUT="$2"; shift 2 ;;
    *) usage ;;
  esac
done
[[ -n "${DB_PUBLIC_IP}" && -f "${SSH_KEY}" && -n "${MYSQL_PASSWORD}" && -n "${OUTPUT}" ]] || usage

mkdir -p "$(dirname "${OUTPUT}")"
SSH=(ssh -n -i "${SSH_KEY}" -o StrictHostKeyChecking=accept-new -o ConnectTimeout=10 "ec2-user@${DB_PUBLIC_IP}")
remote_mysql() {
  local sql="$1"
  "${SSH[@]}" "sudo env MYSQL_PWD='${MYSQL_PASSWORD}' mysql --protocol=socket -uroot --batch --raw rati -e \"${sql}\""
}

ANALYZE_OUTPUT="$(remote_mysql 'ANALYZE TABLE users, friend_relations, notification, event_publication;')"
STATUS_OUTPUT="$(remote_mysql "SHOW GLOBAL STATUS WHERE Variable_name IN ('Threads_connected','Threads_running','Max_used_connections','Created_tmp_tables','Created_tmp_disk_tables','Innodb_row_lock_time','Innodb_row_lock_waits');")"
DRAIN_OUTPUT="$(remote_mysql "SELECT status, COUNT(*) AS count FROM notification GROUP BY status ORDER BY status; SELECT COUNT(*) AS outstanding_publications FROM event_publication WHERE completion_date IS NULL;")"
DIGEST_OUTPUT="$(remote_mysql "SELECT DIGEST_TEXT, COUNT_STAR, ROUND(SUM_TIMER_WAIT/1000000000,2) AS total_ms, ROUND(AVG_TIMER_WAIT/1000000,2) AS avg_ms, SUM_ROWS_EXAMINED FROM performance_schema.events_statements_summary_by_digest WHERE SCHEMA_NAME='rati' ORDER BY SUM_TIMER_WAIT DESC LIMIT 20;")"
INDEX_OUTPUT="$(remote_mysql "SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX FROM information_schema.statistics WHERE TABLE_SCHEMA='rati' ORDER BY TABLE_NAME, INDEX_NAME, SEQ_IN_INDEX;")"
EXPLAIN_USER="$(remote_mysql "EXPLAIN FORMAT=JSON SELECT id, email, nickname, status FROM users WHERE id=UUID_TO_BIN('00000000-0000-0000-0000-000000000001');")"
EXPLAIN_FRIEND="$(remote_mysql "EXPLAIN FORMAT=JSON SELECT * FROM friend_relations WHERE (low_user_id=UUID_TO_BIN('00000000-0000-0000-0000-000000000001') OR high_user_id=UUID_TO_BIN('00000000-0000-0000-0000-000000000001')) AND status='ACCEPTED' ORDER BY friend_relation_id LIMIT 21;")"
EXPLAIN_NOTIFICATION="$(remote_mysql "EXPLAIN FORMAT=JSON SELECT * FROM notification WHERE target_identifier='00000000-0000-0000-0000-000000000001' AND method='FCM' AND status='SENT' ORDER BY created_at DESC LIMIT 21;")"
ANALYZE_USER="$(remote_mysql "EXPLAIN ANALYZE SELECT id, email, nickname, status FROM users WHERE id=UUID_TO_BIN('00000000-0000-0000-0000-000000000001');")"
ANALYZE_FRIEND="$(remote_mysql "EXPLAIN ANALYZE SELECT * FROM friend_relations WHERE (low_user_id=UUID_TO_BIN('00000000-0000-0000-0000-000000000001') OR high_user_id=UUID_TO_BIN('00000000-0000-0000-0000-000000000001')) AND status='ACCEPTED' ORDER BY friend_relation_id LIMIT 21;")"
ANALYZE_NOTIFICATION="$(remote_mysql "EXPLAIN ANALYZE SELECT * FROM notification WHERE target_identifier='00000000-0000-0000-0000-000000000001' AND method='FCM' AND status='SENT' ORDER BY created_at DESC LIMIT 21;")"

cat > "${OUTPUT}" <<EOF
# SQL optimizer snapshot

- Collected: $(date -u +%Y-%m-%dT%H:%M:%SZ)
- Database: rati
- This report is for the isolated load-test database only.

## ANALYZE TABLE

\`\`\`text
${ANALYZE_OUTPUT}
\`\`\`

## Connection and lock status

\`\`\`text
${STATUS_OUTPUT}
\`\`\`

## Delivery and event backlog

\`\`\`text
${DRAIN_OUTPUT}
\`\`\`

## Top statement digests

\`\`\`text
${DIGEST_OUTPUT}
\`\`\`

## Index catalog

\`\`\`text
${INDEX_OUTPUT}
\`\`\`

## Representative execution plans

### User lookup

\`\`\`json
${EXPLAIN_USER}
\`\`\`

### Friend relation lookup

\`\`\`json
${EXPLAIN_FRIEND}
\`\`\`

### Notification inbox

\`\`\`json
${EXPLAIN_NOTIFICATION}
\`\`\`

## EXPLAIN ANALYZE timings

### User lookup

\`\`\`text
${ANALYZE_USER}
\`\`\`

### Friend relation lookup

\`\`\`text
${ANALYZE_FRIEND}
\`\`\`

### Notification inbox

\`\`\`text
${ANALYZE_NOTIFICATION}
\`\`\`

## AI optimizer action

The AI must select candidates from the digest and execution-plan evidence, apply one attributable change per candidate, rerun the identical workload, and record accepted/rejected reasoning in the parent experiment report.
EOF

echo "SQL Markdown report: ${OUTPUT}"
