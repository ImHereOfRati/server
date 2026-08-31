#!/usr/bin/env bash

# Docker 없이 Amazon Linux EC2 에 MySQL 8.0 을 설치합니다.
#
# 필수 환경 변수:
#   MYSQL_ROOT_PASSWORD  설정할 MySQL root 비밀번호
#
# 선택 환경 변수:
#   MYSQL_DATABASE        생성할 데이터베이스 이름 (기본값: rati)
#   MYSQL_MAX_CONNECTIONS 최대 연결 수 (기본값: 30)
#   MYSQL_REPO_RPM_URL    MySQL Yum Repository RPM URL
#                         (기본값: MySQL 8.0 EL9 repository)
#   MYSQL_REPO_KEY_URL    MySQL 패키지 서명 키 URL
#                         (기본값: mysql-2025 GPG key)

set -euo pipefail

: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is required}"

MYSQL_DATABASE="${MYSQL_DATABASE:-rati}"
MYSQL_MAX_CONNECTIONS="${MYSQL_MAX_CONNECTIONS:-30}"
MYSQL_REPO_RPM_URL="${MYSQL_REPO_RPM_URL:-https://dev.mysql.com/get/mysql80-community-release-el9-1.noarch.rpm}"
MYSQL_REPO_KEY_URL="${MYSQL_REPO_KEY_URL:-https://repo.mysql.com/RPM-GPG-KEY-mysql-2025}"
MYSQL_CONFIG_FILE="/etc/my.cnf.d/imhere-loadtest.cnf"

if [[ "${EUID}" -ne 0 ]]; then
  echo "Run this script as root or with sudo." >&2
  exit 1
fi

dnf install -y "${MYSQL_REPO_RPM_URL}"
dnf module disable -y mysql >/dev/null 2>&1 || true
# 8.0.44 이상 패키지 검증에 사용하는 최신 MySQL GPG 키를 등록한다.
rpm --import "${MYSQL_REPO_KEY_URL}"
if [[ -f /etc/yum.repos.d/mysql-community.repo ]]; then
  sed -i 's#RPM-GPG-KEY-mysql-2022#RPM-GPG-KEY-mysql-2025#g' /etc/yum.repos.d/mysql-community.repo
fi
dnf install -y mysql-community-server mysql-community-client

cat > "${MYSQL_CONFIG_FILE}" <<EOF
[mysqld]
max_connections=${MYSQL_MAX_CONNECTIONS}
EOF

# Amazon Linux의 기본 /etc/my.cnf가 my.cnf.d를 include하지 않을 수 있으므로
# 부하 테스트 전용 설정 디렉터리를 명시적으로 읽게 한다.
if ! grep -Fqx '!includedir /etc/my.cnf.d' /etc/my.cnf; then
  printf '\n!includedir /etc/my.cnf.d\n' >> /etc/my.cnf
fi

systemctl enable mysqld
systemctl start mysqld

mysqladmin --protocol=socket ping >/dev/null 2>&1 || {
  echo "MySQL did not become ready." >&2
  systemctl --no-pager --full status mysqld || true
  exit 1
}

TEMP_ROOT_PASSWORD=""
if grep -q 'temporary password' /var/log/mysqld.log 2>/dev/null; then
  TEMP_ROOT_PASSWORD="$(grep 'temporary password' /var/log/mysqld.log | tail -n 1 | awk '{print $NF}')"
fi

if [[ -n "${TEMP_ROOT_PASSWORD}" ]]; then
  mysql --connect-expired-password -uroot -p"${TEMP_ROOT_PASSWORD}" <<SQL
ALTER USER 'root'@'localhost' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
ALTER USER 'root'@'%' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
CREATE DATABASE IF NOT EXISTS \`${MYSQL_DATABASE}\`;
SQL
else
  mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE USER IF NOT EXISTS 'root'@'%' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
ALTER USER 'root'@'%' IDENTIFIED BY '${MYSQL_ROOT_PASSWORD}';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%' WITH GRANT OPTION;
CREATE DATABASE IF NOT EXISTS \`${MYSQL_DATABASE}\`;
SQL
fi

systemctl restart mysqld
mysqladmin -uroot -p"${MYSQL_ROOT_PASSWORD}" ping >/dev/null

echo "MySQL installed and configured."
echo "Database: ${MYSQL_DATABASE}"
echo "Max connections: ${MYSQL_MAX_CONNECTIONS}"
