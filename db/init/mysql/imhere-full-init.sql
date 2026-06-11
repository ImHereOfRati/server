-- ImHere full database initialization script for MySQL 8+
-- Usage:
-- 1. Update database/user names if needed.
-- 2. Execute this file once before application startup.

CREATE DATABASE IF NOT EXISTS rati
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE rati;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS user_agreement;
DROP TABLE IF EXISTS friend_relationships;
DROP TABLE IF EXISTS friend_restrictions;
DROP TABLE IF EXISTS friend_request;
DROP TABLE IF EXISTS notification_history;
DROP TABLE IF EXISTS fcm_token;
DROP TABLE IF EXISTS one_time_tokens;
DROP TABLE IF EXISTS terms;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users (
    id CHAR(36) NOT NULL,
    email VARCHAR(255) NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    role ENUM('NORMAL', 'ADMIN') NOT NULL,
    provider ENUM('KAKAO') NOT NULL,
    status ENUM('PENDING', 'ACTIVE', 'BLOCKED') NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_nickname (nickname)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE terms (
    id BIGINT NOT NULL AUTO_INCREMENT,
    version BIGINT NOT NULL,
    type ENUM('SERVICE', 'PRIVACY', 'LOCATION', 'MARKETING') NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    effective_date DATETIME(6) NOT NULL,
    is_required BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_terms_type_version (type, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_agreement (
    id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    terms_version_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_agreement_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_agreement_terms FOREIGN KEY (terms_version_id) REFERENCES terms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE friend_request (
    friend_request_id CHAR(36) NOT NULL,
    requester_id CHAR(36) NOT NULL,
    receiver_id CHAR(36) NOT NULL,
    message VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (friend_request_id),
    CONSTRAINT fk_friend_request_requester FOREIGN KEY (requester_id) REFERENCES users (id),
    CONSTRAINT fk_friend_request_receiver FOREIGN KEY (receiver_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE friend_restrictions (
    friend_restriction_id CHAR(36) NOT NULL,
    restrictor_id CHAR(36) NULL,
    restricted_id CHAR(36) NULL,
    type ENUM('BLOCK', 'REJECT') NOT NULL,
    expired_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (friend_restriction_id),
    CONSTRAINT fk_friend_restrictions_restrictor FOREIGN KEY (restrictor_id) REFERENCES users (id),
    CONSTRAINT fk_friend_restrictions_restricted FOREIGN KEY (restricted_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE friend_relationships (
    friend_relationship_id CHAR(36) NOT NULL,
    owner_user_id CHAR(36) NOT NULL,
    friend_user_id CHAR(36) NOT NULL,
    friend_alias VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (friend_relationship_id),
    UNIQUE KEY uk_owner_friend (owner_user_id, friend_user_id),
    CONSTRAINT fk_friend_relationships_owner FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT fk_friend_relationships_friend FOREIGN KEY (friend_user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE fcm_token (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    device_type ENUM('AOS', 'IOS') NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notification_history (
    id BIGINT NOT NULL AUTO_INCREMENT,
    receiver_email VARCHAR(255) NOT NULL,
    sender_nickname VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    path VARCHAR(255) NULL,
    is_read BIT(1) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE one_time_tokens (
    token_value VARCHAR(255) NOT NULL,
    username VARCHAR(255) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    PRIMARY KEY (token_value),
    KEY idx_one_time_tokens_expires_at (expires_at),
    KEY idx_one_time_tokens_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Optional: create an application DB user and grant privileges.
-- Replace 'imhere_app' and 'change-me-password' before use.
-- CREATE USER IF NOT EXISTS 'imhere_app'@'%' IDENTIFIED BY 'change-me-password';
-- GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP ON rati.* TO 'imhere_app'@'%';
-- FLUSH PRIVILEGES;

-- Seed default terms.
INSERT INTO terms (
    version,
    type,
    title,
    content,
    effective_date,
    is_required,
    created_at,
    updated_at,
    created_by,
    updated_by
) VALUES
(
    1,
    'SERVICE',
    '서비스 이용약관',
    '본 약관은 ImHere 서비스의 이용 조건과 운영 원칙을 정의합니다.\n\n1. 사용자는 서비스 내 위치 공유, 친구 요청, 알림 기능을 정상 목적에 한해 사용해야 합니다.\n2. 서비스 운영을 방해하거나 타인의 권리를 침해하는 행위는 제한됩니다.\n3. 운영 정책 위반 시 계정 제한 또는 차단이 발생할 수 있습니다.',
    '2026-01-01 00:00:00',
    b'1',
    NOW(6),
    NOW(6),
    'system',
    'system'
),
(
    1,
    'PRIVACY',
    '개인정보 처리방침',
    'ImHere는 서비스 제공을 위해 이메일, 닉네임, 로그인 제공자 정보 등을 처리합니다.\n\n권한 및 개인정보 안내\n1. 위치 기반 기능 사용 시 사용자의 위치 정보가 처리될 수 있습니다.\n2. 친구 기능 사용 시 요청/수락/차단 이력이 저장될 수 있습니다.\n3. 알림 기능 사용 시 기기 토큰과 알림 이력이 저장될 수 있습니다.',
    '2026-01-01 00:00:00',
    b'1',
    NOW(6),
    NOW(6),
    'system',
    'system'
),
(
    1,
    'LOCATION',
    '위치정보 이용약관',
    'ImHere는 사용자 주변 관계 기능 제공을 위해 위치정보를 사용할 수 있습니다.\n\n권한 안내\n1. 위치 권한은 친구 위치 확인 및 근처 사용자 기능 제공에 사용됩니다.\n2. 사용자는 운영체제 설정에서 위치 권한을 변경할 수 있습니다.\n3. 위치 권한 미동의 시 일부 기능 사용이 제한될 수 있습니다.',
    '2026-01-01 00:00:00',
    b'1',
    NOW(6),
    NOW(6),
    'system',
    'system'
),
(
    1,
    'MARKETING',
    '마케팅 정보 수신 동의',
    'ImHere의 신규 기능, 이벤트, 혜택 정보를 알림 또는 기타 수단으로 받을 수 있습니다.\n\n권한 안내\n1. 마케팅 수신 동의는 선택 사항입니다.\n2. 언제든지 수신 동의를 철회할 수 있습니다.\n3. 수신 거부 시에도 서비스 핵심 기능은 계속 사용할 수 있습니다.',
    '2026-01-01 00:00:00',
    b'0',
    NOW(6),
    NOW(6),
    'system',
    'system'
);
