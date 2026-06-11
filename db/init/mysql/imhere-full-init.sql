DROP DATABASE IF EXISTS rati;

CREATE DATABASE rati
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

CREATE TABLE users
(
    id         CHAR(36)                                           NOT NULL,
    email      VARCHAR(255)                                       NOT NULL,
    nickname   VARCHAR(255)                                       NOT NULL,
    role       ENUM ('NORMAL', 'ADMIN')                           NOT NULL,
    provider   ENUM ('KAKAO')                                     NOT NULL,
    status     ENUM ('PENDING', 'ACTIVE', 'BLOCKED', 'WITHDRAWN') NOT NULL,
    created_at DATETIME(6)                                        NOT NULL,
    updated_at DATETIME(6)                                        NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_nickname (nickname)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE terms
(
    id             BIGINT                                               NOT NULL AUTO_INCREMENT,
    version        BIGINT                                               NOT NULL,
    type           ENUM ('SERVICE', 'PRIVACY', 'LOCATION', 'MARKETING') NOT NULL,
    title          VARCHAR(255)                                         NOT NULL,
    content        TEXT                                                 NOT NULL,
    effective_date DATETIME(6)                                          NOT NULL,
    is_required    BIT(1)                                               NOT NULL,
    created_at     DATETIME(6)                                          NOT NULL,
    updated_at     DATETIME(6)                                          NOT NULL,
    created_by     VARCHAR(255)                                         NULL,
    updated_by     VARCHAR(255)                                         NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_terms_type_version (type, version)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE user_agreement
(
    id               CHAR(36)    NOT NULL,
    user_id          CHAR(36)    NOT NULL,
    terms_version_id BIGINT      NOT NULL,
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_agreement_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_agreement_terms FOREIGN KEY (terms_version_id) REFERENCES terms (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE friend_request
(
    friend_request_id CHAR(36)                     NOT NULL,
    requester_id      CHAR(36)                     NOT NULL,
    receiver_id       CHAR(36) NOT NULL,
    message           VARCHAR(255)                 NOT NULL,
    created_at        DATETIME(6)                  NOT NULL,
    updated_at        DATETIME(6)                  NOT NULL,
    PRIMARY KEY (friend_request_id),
    CONSTRAINT fk_friend_request_requester FOREIGN KEY (requester_id) REFERENCES users (id),
    CONSTRAINT fk_friend_request_receiver FOREIGN KEY (receiver_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE friend_restrictions
(
    friend_restriction_id CHAR(36) NOT NULL,
    restrictor_id         CHAR(36) NULL,
    restricted_id         CHAR(36) NULL,
    type                  ENUM ('BLOCK', 'REJECT')     NOT NULL,
    expired_at            DATETIME(6)                  NULL,
    created_at            DATETIME(6)                  NOT NULL,
    updated_at            DATETIME(6)                  NOT NULL,
    PRIMARY KEY (friend_restriction_id),
    CONSTRAINT fk_friend_restrictions_restrictor FOREIGN KEY (restrictor_id) REFERENCES users (id),
    CONSTRAINT fk_friend_restrictions_restricted FOREIGN KEY (restricted_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE friend_relationships
(
    friend_relationship_id CHAR(36)    NOT NULL,
    owner_user_id          CHAR(36)    NOT NULL,
    friend_user_id         CHAR(36)    NOT NULL,
    friend_alias           VARCHAR(20) NOT NULL,
    created_at             DATETIME(6) NOT NULL,
    updated_at             DATETIME(6) NOT NULL,
    PRIMARY KEY (friend_relationship_id),
    UNIQUE KEY uk_owner_friend (owner_user_id, friend_user_id),
    CONSTRAINT fk_friend_relationships_owner FOREIGN KEY (owner_user_id) REFERENCES users (id),
    CONSTRAINT fk_friend_relationships_friend FOREIGN KEY (friend_user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE fcm_token
(
    id          BIGINT              NOT NULL AUTO_INCREMENT,
    token       VARCHAR(255)        NOT NULL,
    email       VARCHAR(255)        NOT NULL,
    device_type ENUM ('AOS', 'IOS') NOT NULL,
    created_at  DATETIME(6)         NOT NULL,
    updated_at  DATETIME(6)         NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE notification_history
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    receiver_email  VARCHAR(255) NOT NULL,
    sender_nickname VARCHAR(255) NOT NULL,
    title           VARCHAR(255) NOT NULL,
    body            VARCHAR(255) NOT NULL,
    type            VARCHAR(255) NOT NULL,
    path            VARCHAR(255) NULL,
    is_read         BIT(1)       NOT NULL,
    created_at      DATETIME(6)  NOT NULL,
    updated_at      DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE one_time_tokens
(
    token_value VARCHAR(255) NOT NULL,
    username    VARCHAR(255) NOT NULL,
    issued_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at  TIMESTAMP    NOT NULL,
    PRIMARY KEY (token_value),
    KEY idx_one_time_tokens_expires_at (expires_at),
    KEY idx_one_time_tokens_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Optional: create an application DB user and grant privileges.
-- Replace 'imhere_app' and 'change-me-password' before use.
-- CREATE USER IF NOT EXISTS 'imhere_app'@'%' IDENTIFIED BY 'change-me-password';
-- GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP ON rati.* TO 'imhere_app'@'%';
-- FLUSH PRIVILEGES;

-- Seed default terms.
INSERT INTO terms (version,
                   type,
                   title,
                   content,
                   effective_date,
                   is_required,
                   created_at,
                   updated_at,
                   created_by,
                   updated_by)
VALUES (1,
        'SERVICE',
        '서비스 이용약관',
        '본 약관은 Rati(이하 "서비스")의 이용과 관련하여 서비스 제공자와 이용자 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.

      제1조 (서비스의 제공)
      1. 서비스는 위치 기반 도착·출발 알림, 친구 관리, 알림 전송 등의 기능을 제공합니다.
      2. 서비스 제공자는 서비스 운영상 필요에 따라 제공 기능을 변경할 수 있습니다.

      제2조 (이용자의 의무)
      1. 이용자는 관련 법령 및 본 약관을 준수하여야 합니다.
      2. 이용자는 타인의 개인정보 또는 위치정보를 무단으로 수집·이용·공유하여서는 안 됩니다.
      3. 이용자는 서비스의 정상적인 운영을 방해하는 행위를 하여서는 안 됩니다.

      제3조 (서비스 이용 제한)
      서비스 제공자는 법령 또는 본 약관을 위반한 이용자에 대하여 서비스 이용을 제한하거나 계정을 정지할 수 있습니다.

      제4조 (면책)
      1. 서비스 제공자는 천재지변, 통신 장애 등 불가항력으로 인한 서비스 중단에 대하여 책임을 지지 않습니다.
      2. 이용자의 기기 설정, 네트워크 환경 또는 운영체제 정책으로 인해 알림이 정상 동작하지 않는 경우 책임을 지지 않습니다.',
        '2026-01-01 00:00:00',
        b'1',
        NOW(6),
        NOW(6),
        'system',
        'system'),
       (1,
        'PRIVACY',
        '개인정보 처리방침',
        '서비스는 개인정보 보호법에 따라 이용자의 개인정보를 보호합니다.

    1. 수집 항목
    - 이메일 주소
    - 닉네임
    - 로그인 제공자 정보
    - 서비스 이용 기록

    2. 수집 목적
    - 회원 식별 및 인증
    - 친구 기능 제공
    - 서비스 운영 및 고객 문의 대응

    3. 보유 및 이용 기간
    - 회원 탈퇴 시 지체 없이 파기합니다.
    - 관계 법령에 따라 보관이 필요한 경우 해당 기간 동안 보관합니다.

    4. 개인정보 제공
    서비스는 이용자의 동의 없이 개인정보를 제3자에게 제공하지 않습니다.

    5. 이용자의 권리
    이용자는 언제든지 개인정보 열람, 정정, 삭제 및 처리 정지를 요청할 수 있습니다.',
        '2026-01-01 00:00:00',
        b'1',
        NOW(6),
        NOW(6),
        'system',
        'system'),
       (1,
        'LOCATION',
        '위치정보 이용약관',
        '서비스는 위치정보의 보호 및 이용 등에 관한 법률에 따라 개인위치정보를 처리합니다.

    1. 위치정보 이용 목적
    - 도착·출발 알림 제공
    - 친구 간 위치 기반 기능 제공
    - 위치 기반 서비스 품질 개선

    2. 수집하는 위치정보
    - 모바일 기기에서 제공하는 GPS 또는 네트워크 기반 위치정보

    3. 보유 기간
    - 실시간 위치정보는 서비스 제공 목적 달성 후 지체 없이 파기합니다.
    - 법령에 따라 보관이 필요한 경우 해당 기간 동안 보관합니다.

    4. 이용자의 권리
    - 이용자는 언제든지 위치정보 제공 동의를 철회할 수 있습니다.
    - 이용자는 위치정보 이용 내역을 열람할 수 있습니다.

    5. 동의 거부
    위치정보 제공에 동의하지 않을 수 있으며, 이 경우 위치 기반 기능의 이용이 제한될 수 있습니다.',
        '2026-01-01 00:00:00',
        b'1',
        NOW(6),
        NOW(6),
        'system',
        'system'),
       (1,
        'MARKETING',
        '마케팅 정보 수신 동의',
        '서비스는 신규 기능, 이벤트, 혜택 및 프로모션 정보를 제공하기 위하여 광고성 정보를 발송할 수 있습니다.

    1. 수신 방법
    - 푸시 알림
    - 이메일

    2. 동의 여부
    - 본 동의는 선택 사항입니다.
    - 동의하지 않아도 서비스 이용에는 제한이 없습니다.

    3. 동의 철회
    - 이용자는 언제든지 설정 화면 또는 고객 문의를 통해 수신 동의를 철회할 수 있습니다.

    4. 광고성 정보 발송
    - 서비스는 정보통신망법 등 관련 법령을 준수하여 광고성 정보를 발송합니다.',
        '2026-01-01 00:00:00',
        b'0',
        NOW(6),
        NOW(6),
        'system',
        'system');
