#!/usr/bin/env bash

set -euo pipefail

: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD가 필요합니다.}"

MYSQL_DATABASE="${MYSQL_DATABASE:-rati}"
MYSQL_MAX_CONNECTIONS="${MYSQL_MAX_CONNECTIONS:-30}"
MYSQL_REPO_RPM_URL="${MYSQL_REPO_RPM_URL:-https://dev.mysql.com/get/mysql80-community-release-el9-1.noarch.rpm}"
MYSQL_REPO_KEY_URL="${MYSQL_REPO_KEY_URL:-https://repo.mysql.com/RPM-GPG-KEY-mysql-2025}"
MYSQL_CONFIG_FILE="/etc/my.cnf.d/imhere-loadtest.cnf"

require_root() {
  if [[ "${EUID}" -ne 0 ]]; then
    echo "root 권한으로 실행하거나 sudo를 사용해야 합니다." >&2
    exit 1
  fi
}

install_mysql_repository() {
  dnf install -y "${MYSQL_REPO_RPM_URL}"
  dnf module disable -y mysql >/dev/null 2>&1 || true
  rpm --import "${MYSQL_REPO_KEY_URL}"

  if [[ -f /etc/yum.repos.d/mysql-community.repo ]]; then
    sed -i 's#RPM-GPG-KEY-mysql-2022#RPM-GPG-KEY-mysql-2025#g' \
      /etc/yum.repos.d/mysql-community.repo
  fi
}

install_mysql_packages() {
  dnf install -y mysql-community-server mysql-community-client
}

write_mysql_config() {
  mkdir -p "$(dirname "${MYSQL_CONFIG_FILE}")"
  cat > "${MYSQL_CONFIG_FILE}" <<EOF
[mysqld]
max_connections=${MYSQL_MAX_CONNECTIONS}
EOF

  if ! grep -Fqx '!includedir /etc/my.cnf.d' /etc/my.cnf; then
    printf '\n!includedir /etc/my.cnf.d\n' >> /etc/my.cnf
  fi
}

start_mysql() {
  systemctl enable mysqld
  systemctl start mysqld
}

wait_for_mysql() {
  if ! mysqladmin --protocol=socket ping >/dev/null 2>&1; then
    echo "MySQL이 준비되지 않았습니다." >&2
    systemctl --no-pager --full status mysqld || true
    exit 1
  fi
}

temporary_root_password() {
  if grep -q 'temporary password' /var/log/mysqld.log 2>/dev/null; then
    grep 'temporary password' /var/log/mysqld.log | tail -n 1 | awk '{print $NF}'
  fi
}

configure_root_account() {
  local temporary_password="$1"

  if [[ -n "${temporary_password}" ]]; then
    mysql --connect-expired-password -uroot -p"${temporary_password}" <<SQL
ALTER USER 'root'@'localhost' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
ALTER USER 'root'@'%' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
CREATE DATABASE IF NOT EXISTS \`${MYSQL_DATABASE}\`;
SQL
    return
  fi

  mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
ALTER USER 'root'@'%' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
CREATE DATABASE IF NOT EXISTS \`${MYSQL_DATABASE}\`;
SQL
}

restart_and_verify_mysql() {
  systemctl restart mysqld
  mysqladmin -uroot -p"${MYSQL_ROOT_PASSWORD}" ping >/dev/null
}

print_summary() {
  echo "MySQL 설치 및 설정이 완료되었습니다."
  echo "데이터베이스: ${MYSQL_DATABASE}"
  echo "최대 연결 수: ${MYSQL_MAX_CONNECTIONS}"
}

main() {
  require_root
  install_mysql_repository
  install_mysql_packages
  write_mysql_config
  start_mysql
  wait_for_mysql
  configure_root_account "$(temporary_root_password)"
  restart_and_verify_mysql
  print_summary
}

main "$@"
