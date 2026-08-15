DROP DATABASE IF EXISTS rati;

CREATE DATABASE rati
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE rati;

SET time_zone = '+09:00';

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS user_agreement;
DROP TABLE IF EXISTS friend_relations;
DROP TABLE IF EXISTS friend_relationships;
DROP TABLE IF EXISTS friend_restrictions;
DROP TABLE IF EXISTS friend_request;
DROP TABLE IF EXISTS notification;
DROP TABLE IF EXISTS fcm_token;
DROP TABLE IF EXISTS one_time_tokens;
DROP TABLE IF EXISTS event_publication;
DROP TABLE IF EXISTS terms;
DROP TABLE IF EXISTS users;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE users
(
    id                    BINARY(16)                                         NOT NULL,
    email                 VARCHAR(255)                                       NOT NULL,
    nickname              VARCHAR(255)                                       NOT NULL,
    role                  ENUM ('NORMAL', 'ADMIN')                           NOT NULL,
    provider              ENUM ('KAKAO', 'GOOGLE', 'APPLE')                  NOT NULL,
    status                ENUM ('PENDING', 'ACTIVE', 'BLOCKED', 'WITHDRAWN') NOT NULL,
    oidc_subject          VARCHAR(255)                                       NULL,
    refresh_token_version BIGINT                                             NOT NULL DEFAULT 0,
    created_at            DATETIME(6)                                        NOT NULL,
    updated_at            DATETIME(6)                                        NOT NULL,
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
    id               BINARY(16)   NOT NULL,
    user_id          BINARY(16)   NOT NULL,
    terms_version_id BIGINT       NOT NULL,
    action           VARCHAR(20)  NOT NULL,
    occurred_at      DATETIME(6)  NOT NULL,
    created_by       VARCHAR(255) NULL,
    updated_by       VARCHAR(255) NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_user_agreement_history (user_id, terms_version_id, occurred_at),
    CONSTRAINT fk_user_agreement_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_agreement_terms FOREIGN KEY (terms_version_id) REFERENCES terms (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- 친구 관계는 요청, 수락, 거절, 차단, 취소 상태를 하나의 pair row로 관리한다.
-- low/high 정렬은 양방향 친구 관계를 하나의 유니크 pair로 묶기 위한 식별 기준이다.
CREATE TABLE friend_relations
(
    friend_relation_id BINARY(16)                                                      NOT NULL,
    low_user_id        BINARY(16)                                                      NOT NULL,
    high_user_id       BINARY(16)                                                      NOT NULL,
    status             ENUM ('REQUESTED', 'ACCEPTED', 'REJECTED', 'BLOCKED', 'CANCEL') NOT NULL,
    initiated_user_id  BINARY(16)                                                      NOT NULL,
    message            VARCHAR(255)                                                    NULL,
    low_alias          VARCHAR(10)                                                     NULL,
    high_alias         VARCHAR(10)                                                     NULL,
    expired_at         DATETIME(6)                                                     NULL,
    created_at         DATETIME(6)                                                     NOT NULL,
    updated_at         DATETIME(6)                                                     NOT NULL,
    PRIMARY KEY (friend_relation_id),
    UNIQUE KEY uk_friend_pair (low_user_id, high_user_id),
    KEY idx_friend_relations_low (low_user_id, status),
    KEY idx_friend_relations_high (high_user_id, status),
    KEY idx_friend_relations_expired_at (expired_at),
    CONSTRAINT fk_friend_relations_low FOREIGN KEY (low_user_id) REFERENCES users (id),
    CONSTRAINT fk_friend_relations_high FOREIGN KEY (high_user_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- FCM 토큰의 소유자는 이메일이 아니라 user id로 식별한다.
-- 현재 서버 로직은 사용자당 하나의 활성 토큰을 유지한다.
CREATE TABLE fcm_token
(
    id          BIGINT              NOT NULL AUTO_INCREMENT,
    token       VARCHAR(255)        NOT NULL,
    owner_id    BINARY(16)          NOT NULL,
    device_type ENUM ('AOS', 'IOS') NOT NULL,
    created_at  DATETIME(6)         NOT NULL,
    updated_at  DATETIME(6)         NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fcm_token_owner_id (owner_id),
    CONSTRAINT fk_fcm_token_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- notification은 알림 발송과 알림함 표시의 생명주기를 저장한다.
-- PENDING, SENT, FAILED, DEAD 상태와 dedupe key로 중복 발송과 재시도를 제어한다.
CREATE TABLE notification
(
    id                BIGINT                                      NOT NULL AUTO_INCREMENT,
    dedupe_key        VARCHAR(120)                                NOT NULL,
    target_identifier VARCHAR(255)                                NOT NULL,
    method            ENUM ('SMS', 'FCM')                         NOT NULL,
    sender_alias      VARCHAR(255)                                NOT NULL,
    type              VARCHAR(255)                                NOT NULL,
    title             VARCHAR(255)                                NOT NULL,
    body              VARCHAR(500)                                NOT NULL,
    extra_data        VARCHAR(2000)                               NOT NULL,
    status            ENUM ('PENDING', 'SENT', 'FAILED', 'DEAD')  NOT NULL,
    attempts          INT                                         NOT NULL DEFAULT 0,
    last_error        VARCHAR(500)                                NULL,
    sent_at           DATETIME(6)                                 NULL,
    is_read           BIT(1)                                      NOT NULL DEFAULT b'0',
    created_at        DATETIME(6)                                 NOT NULL,
    updated_at        DATETIME(6)                                 NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_dedupe_key (dedupe_key),
    KEY idx_notification_inbox (target_identifier, method, status, created_at),
    KEY idx_notification_status (status, created_at)
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

CREATE TABLE event_publication
(
    id                     BINARY(16)    NOT NULL,
    listener_id            VARCHAR(512)  NOT NULL,
    event_type             VARCHAR(512)  NOT NULL,
    serialized_event       VARCHAR(4000) NOT NULL,
    publication_date       TIMESTAMP(6)  NOT NULL,
    completion_date        TIMESTAMP(6)  NULL DEFAULT NULL,
    status                 VARCHAR(20)   NULL,
    completion_attempts    INT           NULL,
    last_resubmission_date TIMESTAMP(6)  NULL DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX event_publication_by_completion_date_idx (completion_date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

-- Optional: create an application DB user and grant privileges.
-- Replace 'imhere_app' and 'change-me-password' before use.
-- CREATE USER IF NOT EXISTS 'imhere_app'@'%' IDENTIFIED BY 'change-me-password';
-- GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, DROP ON rati.* TO 'imhere_app'@'%';
-- FLUSH PRIVILEGES;

-- Seed default terms. Version and enum values are intentionally kept compatible with the current server enum.
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
        '이 약관은 Imhere 서비스의 이용 조건과 회원 및 운영자의 권리, 의무, 책임사항을 정합니다.

1. 서비스의 제공
- 서비스는 위치 기반 도착 및 출발 알림, 친구 관계 관리, 알림함, 푸시 알림, 필요한 경우 문자 알림, 계정 인증 및 보안 기능을 제공합니다.
- 알림에는 화면 이동에 필요한 최소한의 유형 정보와 부가 데이터가 포함될 수 있습니다.
- 운영자는 서비스 안정성, 보안, 정책 변경, 법령 준수 또는 기능 개선을 위해 서비스의 일부를 변경하거나 중단할 수 있습니다.

2. 회원의 의무
- 회원은 관계 법령과 이 약관, 서비스 화면의 안내를 준수해야 합니다.
- 회원은 타인의 계정, 개인정보, 위치정보 또는 알림 정보를 무단으로 수집, 이용, 공유해서는 안 됩니다.
- 회원은 허위 정보 입력, 비정상적인 접근, 자동화된 요청, 시스템 장애 유발 등 서비스 운영을 방해하는 행위를 해서는 안 됩니다.

3. 친구 및 알림 기능
- 친구 요청, 수락, 차단, 취소 등 관계 상태에 따라 알림 수신 범위가 달라질 수 있습니다.
- 알림은 기기 설정, 네트워크 상태, 운영체제 정책, FCM 토큰 등록 여부, 문자 발송 사업자 상태에 따라 지연되거나 전달되지 않을 수 있습니다.
- 운영자는 중복 발송 방지, 실패 재시도, 알림함 표시를 위해 필요한 범위에서 알림 처리 기록을 보관할 수 있습니다.

4. 이용 제한
- 운영자는 회원이 법령, 약관 또는 서비스 정책을 위반한 경우 서비스 이용을 제한하거나 계정을 정지할 수 있습니다.
- 부정 사용, 보안 사고, 타인의 권리 침해가 의심되는 경우 필요한 범위에서 관련 기록을 확인할 수 있습니다.

5. 면책
- 천재지변, 통신 장애, 외부 플랫폼 장애, 회원 기기 설정 또는 권한 거부로 인해 서비스가 정상 동작하지 않을 수 있습니다.
- 위치 및 알림 기능은 보조적인 편의 기능이며, 긴급 구조나 안전 보장을 목적으로 제공되지 않습니다.',
        '2026-06-29 00:00:00',
        b'1',
        NOW(6),
        NOW(6),
        'system',
        'system'),
       (1,
        'PRIVACY',
        '개인정보 처리방침',
        'Imhere는 개인정보 보호법 등 관련 법령에 따라 회원의 개인정보를 보호합니다.

1. 수집하는 개인정보
- 계정 정보: 이메일 주소, 닉네임, 로그인 제공자, 제공자 식별값, 회원 상태, 권한, refresh token version
- 인증 및 보안 정보: 일회용 토큰 값, 사용자 식별자, 발급 시각, 만료 시각, 로그인 및 인증 처리 기록
- 친구 기능 정보: 친구 관계 식별자, 요청자와 상대방 식별자, 관계 상태, 요청 메시지, 별칭, 요청 만료 시각
- 알림 정보: 알림 대상 식별자, 발송 수단, 발신자 별칭, 알림 유형, 제목, 본문, 화면 이동용 부가 데이터, 발송 상태, 시도 횟수, 오류 내용, 발송 시각, 읽음 여부
- 기기 및 푸시 정보: FCM 토큰, 기기 종류, 토큰 등록 및 갱신 시각
- 위치 기능 정보: 위치 권한이 허용되고 기능이 활성화된 경우 도착 및 출발 판정, 친구 위치 기반 알림 제공에 필요한 위치 관련 정보
- 서비스 이용 및 분석 정보: 화면 이동, 기능 사용, 오류, 앱 또는 브라우저 정보, 기기 정보, 대략적인 접속 환경, 동의 상태. 단, 분석 정보는 선택 동의가 있는 경우에만 활성화하며 이름, 이메일, 전화번호, 정확한 좌표, 메시지 본문 등 직접 식별 정보는 분석 이벤트로 전송하지 않는 것을 원칙으로 합니다.

2. 이용 목적
- 회원 가입, 로그인, 본인 확인, 계정 상태 관리
- 친구 요청, 수락, 차단, 별칭 관리 등 친구 기능 제공
- 위치 기반 도착 및 출발 판정, 알림 생성, 알림함 제공
- FCM 푸시 또는 문자 알림 발송, 발송 실패 재시도, 중복 발송 방지
- 보안, 장애 대응, 부정 이용 방지, 고객 문의 처리
- 선택 동의가 있는 경우 서비스 이용 통계, 기능 개선, A/B 테스트, 마케팅 성과 측정

3. 보유 및 이용 기간
- 회원 정보는 회원 탈퇴 시 지체 없이 삭제하거나 복구할 수 없도록 익명화합니다. 다만 법령상 보관 의무가 있거나 분쟁 대응을 위해 필요한 경우 해당 목적과 기간에 한해 분리 보관할 수 있습니다.
- FCM 토큰은 새 토큰 등록, 회원 탈퇴, 앱 삭제 또는 토큰 무효 처리 시 삭제합니다.
- 일회용 인증 토큰은 만료 후 삭제 대상이 되며, 인증과 보안 목적 범위에서만 사용합니다.
- 친구 관계, 알림함, 알림 발송 기록, 동의 이력은 서비스 제공, 이용자 권리 확인, 분쟁 대응에 필요한 기간 동안 보관하며 회원 탈퇴 또는 보관 목적 달성 시 삭제하거나 익명화합니다.
- 위치 관련 정보는 위치 기반 기능 제공 목적 달성 후 지체 없이 삭제하거나, 알림함 표시 및 분쟁 대응에 필요한 최소 범위에서만 보관합니다.

4. 제3자 제공
- Imhere는 회원의 사전 동의 없이 개인정보를 제3자에게 제공하지 않습니다. 다만 법령에 근거가 있거나 수사기관 등 적법한 요청이 있는 경우 예외적으로 제공할 수 있습니다.

5. 처리 위탁 및 외부 서비스 이용
- 소셜 로그인 제공자: Kakao, Google, Apple. 로그인 인증 및 계정 식별을 위해 이메일, 닉네임, 제공자 식별값 등 필요한 정보를 처리합니다.
- 푸시 알림: Google Firebase Cloud Messaging. FCM 토큰, 알림 제목, 본문, 화면 이동용 부가 데이터를 푸시 발송 목적으로 처리합니다.
- 문자 알림: Solapi 등 문자 발송 사업자. 문자 알림 발송이 필요한 경우 전화번호와 메시지 내용을 처리할 수 있습니다.
- 지도 및 위치 보조 기능: Naver Maps 등 지도 API 제공자. 주소 검색, 지도 표시, 위치 보조 기능 제공에 필요한 요청 정보를 처리할 수 있습니다.
- 선택 분석 도구: Google Analytics 4, Microsoft Clarity 등. 선택 동의가 있는 경우 서비스 이용 이벤트, 브라우저 및 기기 정보, 대략적인 접속 환경을 처리할 수 있습니다.
- 외부 서비스 제공자의 명칭, 처리 항목, 보유 기간은 서비스 운영 환경에 따라 변경될 수 있으며, 중요한 변경이 있는 경우 공지하거나 동의를 다시 받을 수 있습니다.

6. 이용자의 권리
- 회원은 개인정보 열람, 정정, 삭제, 처리 정지, 동의 철회, 회원 탈퇴를 요청할 수 있습니다.
- 선택 동의는 철회해도 기본 서비스 이용에 제한이 없습니다. 다만 해당 선택 기능, 마케팅 수신, 분석 처리 또는 맞춤형 개선 기능은 중단될 수 있습니다.
- 위치 권한 또는 위치정보 이용 동의를 철회하면 위치 기반 알림 기능 이용이 제한될 수 있습니다.',
        '2026-06-29 00:00:00',
        b'1',
        NOW(6),
        NOW(6),
        'system',
        'system'),
       (1,
        'LOCATION',
        '위치정보 이용약관',
        'Imhere는 위치정보의 보호 및 이용 등에 관한 법률 등 관련 법령에 따라 개인위치정보를 처리합니다.

1. 이용 목적
- 도착 및 출발 판정, 위치 기반 알림 생성
- 친구 관계에 기반한 위치 알림 제공
- 사용자가 설정한 위치 기반 기능의 정상 동작 확인
- 위치 기반 서비스의 오류 대응 및 품질 개선

2. 수집 및 이용하는 위치정보
- 모바일 기기에서 제공하는 GPS, 네트워크, 운영체제 위치 서비스 기반 위치정보
- 도착 및 출발 판정 결과, 위치 기반 알림 생성 시각, 알림 처리 상태
- 위치 권한 상태, 위치 기능 활성화 여부 등 기능 제공에 필요한 설정 정보

3. 처리 원칙
- 위치정보는 사용자가 위치 권한을 허용하고 위치 기반 기능을 사용하는 경우에만 처리합니다.
- 친구에게 제공되는 알림은 관계 상태와 사용자가 설정한 범위 안에서만 생성됩니다.
- 위치정보는 긴급 구조, 실시간 감시, 안전 보장을 목적으로 제공되지 않습니다.

4. 보유 및 이용 기간
- 실시간 위치정보는 위치 기반 기능 제공 목적 달성 후 지체 없이 삭제하거나, 알림함 표시 및 장애 대응에 필요한 최소 정보만 보관합니다.
- 위치 기반 알림 기록은 알림함 제공, 발송 확인, 분쟁 대응에 필요한 기간 동안 보관할 수 있습니다.
- 회원 탈퇴, 위치 동의 철회 또는 위치 기능 삭제 시 관련 위치정보는 지체 없이 삭제하거나 복구할 수 없도록 익명화합니다.
- 법령상 보관 의무가 있는 경우 해당 기간 동안 분리 보관할 수 있습니다.

5. 제3자 제공 및 위탁
- Imhere는 회원의 동의 없이 개인위치정보를 제3자에게 제공하지 않습니다.
- 지도 표시, 주소 검색, 위치 보조 기능 제공을 위해 지도 API 제공자에게 필요한 요청 정보가 전송될 수 있습니다.
- 푸시 또는 문자 알림 발송 과정에서 위치 기반 이벤트 결과가 알림 제목, 본문 또는 부가 데이터로 전송될 수 있습니다.

6. 이용자의 권리
- 회원은 언제든지 위치정보 이용 동의를 철회하거나 기기 설정에서 위치 권한을 변경할 수 있습니다.
- 회원은 위치정보 이용 및 제공 사실 확인자료의 열람, 고지, 정정 또는 삭제를 요청할 수 있습니다.
- 위치 동의를 철회하면 위치 기반 도착 및 출발 알림, 친구 위치 알림 등 관련 기능 이용이 제한될 수 있습니다.',
        '2026-06-29 00:00:00',
        b'1',
        NOW(6),
        NOW(6),
        'system',
        'system'),
       (1,
        'MARKETING',
        '마케팅 및 서비스 분석 활용 동의',
        '이 동의는 선택 사항입니다. 동의하지 않아도 기본 서비스 이용에는 제한이 없습니다.

1. 이용 목적
- 신규 기능, 이벤트, 혜택, 공지성 프로모션 안내
- 서비스 이용 통계 작성, 기능 개선, 오류 흐름 분석
- 웹 및 앱 화면의 사용성 개선, A/B 테스트, 마케팅 성과 측정
- 회원 관심사에 맞춘 안내와 서비스 품질 개선

2. 처리 항목
- 계정 및 수신 정보: 이메일 주소, 닉네임, 푸시 토큰, 기기 종류, 수신 동의 상태
- 서비스 이용 정보: 화면 이름, 버튼 클릭, 기능 사용 여부, 오류 이벤트, 접속 환경, 앱 또는 브라우저 정보, 유입 경로
- 캠페인 정보: 이벤트 참여 여부, 알림 수신 및 반응 여부, 혜택 제공 이력
- 분석 이벤트에는 이름, 이메일, 전화번호, 정확한 좌표, 친구 식별 정보, 메시지 본문 등 직접 식별 정보 또는 민감한 내용을 포함하지 않는 것을 원칙으로 합니다.

3. 수신 및 활용 방법
- 푸시 알림, 이메일, 앱 내 알림 또는 서비스 화면을 통해 안내할 수 있습니다.
- 선택 동의가 있는 경우 Google Analytics 4, Microsoft Clarity 등 분석 도구가 활성화될 수 있습니다.
- 광고 개인화, 외부 광고 식별자 연계, 제3자 광고 네트워크 제공은 별도 동의 또는 법령상 근거가 있는 경우에만 수행합니다.

4. 보유 및 이용 기간
- 동의 철회 또는 회원 탈퇴 시 마케팅 수신과 선택 분석 처리를 중단하고 관련 개인정보를 삭제하거나 익명화합니다.
- 이미 발송된 이력, 동의 및 철회 이력, 분쟁 대응에 필요한 기록은 관련 법령과 내부 보관 기준에 따라 필요한 기간 동안 보관할 수 있습니다.

5. 동의 철회
- 회원은 언제든지 설정 화면 또는 고객 문의를 통해 마케팅 수신 및 서비스 분석 활용 동의를 철회할 수 있습니다.
- 동의 철회 후에도 필수 공지, 보안 알림, 서비스 이용에 필요한 거래성 알림은 발송될 수 있습니다.',
        '2026-06-29 00:00:00',
        b'0',
        NOW(6),
        NOW(6),
        'system',
        'system')
ON DUPLICATE KEY UPDATE
    title          = VALUES(title),
    content        = VALUES(content),
    effective_date = VALUES(effective_date),
    is_required    = VALUES(is_required),
    updated_at     = NOW(6),
    updated_by     = VALUES(updated_by);
