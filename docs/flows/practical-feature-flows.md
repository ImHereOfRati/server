# 기능별 실전 흐름

이 문서는 기능을 화면 이름이 아니라 실제 사용 흐름 단위로 빠르게 훑기 위한 보조 문서다. 세부 규칙의 정본은 각 서버 flow/security 문서와 코드다.

---

## 읽는 법

| 순서 | 먼저 보는 이유 |
|---|---|
| auth -> permission -> geofence | 로그인부터 자동 전송 준비까지 서비스 진입 축이 이어진다 |
| trigger -> FCM -> record | 백그라운드 이벤트, 큐, 푸시, 이력 반영이 연속된 하나의 파이프라인이다 |
| friend -> setting | 관계 관리와 사용자 설정이 일상 사용 시 가장 자주 교차한다 |

---

## 기능 지도

### 1. Auth / Login / Terms

| 항목 | 내용 |
|---|---|
| 시작점 | `auth_view`, `auth_view_model`, `auth_service`, `auth_state_provider` |
| 흐름 | 로그인 화면 -> nonce 생성 -> OIDC SDK ID Token 획득 -> 서버 로그인/회원가입 -> secure storage 저장 -> FCM 토큰 등록 -> pending/active 분기 |
| 서버에서 같이 봐야 할 문서 | [oidc-login.md](oidc-login.md), [oidc-signup-activation.md](oidc-signup-activation.md), [token-refresh.md](token-refresh.md), [../security/oauth.md](../security/oauth.md), [../security/jwt.md](../security/jwt.md) |

### 2. Permission Onboarding

| 항목 | 내용 |
|---|---|
| 시작점 | `auto_send_readiness_provider`, `location_permission_gate`, `user_permission_prep_view` |
| 흐름 | 위치 서비스/권한/배터리 최적화 상태 계산 -> 준비 미완료면 `/user-permission` 이동 -> create 권한과 auto-send 권한을 분리 안내 |
| 서버에서 같이 봐야 할 문서 | [notification-pipeline.md](notification-pipeline.md), [fcm-token-failure-chain.md](fcm-token-failure-chain.md) |

### 3. Geofence Create / Edit / Activate

| 항목 | 내용 |
|---|---|
| 시작점 | `geofence_enroll_view`, `geofence_enroll_view_model`, `geofence_view_model`, `native_geofence_registrar_interface` |
| 흐름 | 지오펜스 등록/수정 진입 -> 권한 게이트 통과 -> 위치/반경/메시지/수신자 입력 -> 로컬 row 저장 -> 서버 수신자 매핑 저장 -> 활성 상태면 OS geofence 등록 |
| 서버에서 같이 봐야 할 문서 | [notification-pipeline.md](notification-pipeline.md), [../architecture/architecture.md](../architecture/architecture.md) |

### 4. Geofence Trigger / Delivery / Retry

| 항목 | 내용 |
|---|---|
| 시작점 | `geofence_background_callback`, `geofence_background_runtime`, `geofence_delivery_pipeline` |
| 흐름 | 디바이스 geofence 이벤트 수신 -> background runtime 기동 -> 수신자 해석 -> snapshot 생성 -> queue 적재 -> 즉시 전송 시도 -> 실패 시 retry scheduler -> terminal failure 기록 |
| 서버에서 같이 봐야 할 문서 | [notification-pipeline.md](notification-pipeline.md), [rabbitmq-dlq-replay.md](rabbitmq-dlq-replay.md), [fcm-token-failure-chain.md](fcm-token-failure-chain.md) |

### 5. FCM Notification Lifecycle

| 항목 | 내용 |
|---|---|
| 시작점 | `firebase_cloud_message_service`, `fcm_message_handler`, `fcm_token_service` |
| 흐름 | 앱 시작 후 권한 요청 -> 로그인 후 FCM 토큰 생성/재등록 -> foreground 수신 + local notification -> payload path 추출 -> router 준비 후 이동 |
| 서버에서 같이 봐야 할 문서 | [fcm-token-failure-chain.md](fcm-token-failure-chain.md), [notification-pipeline.md](notification-pipeline.md), [rabbitmq-dlq-replay.md](rabbitmq-dlq-replay.md) |

### 6. Friend / Contact / Restriction

| 항목 | 내용 |
|---|---|
| 시작점 | `contact_view`, `contact_view_model`, `friend_request_view_model`, `friend_restriction_view_model` |
| 흐름 | 연락처 권한 확인 -> 로컬 연락처와 서버 친구 병합 -> 검색으로 서버 사용자 탐색 -> 요청 전송 -> FCM 알림 -> 수락/거절 -> 차단 목록 관리 |
| 서버에서 같이 봐야 할 문서 | [friend-request.md](friend-request.md), [friend-block.md](friend-block.md), [notification-pipeline.md](notification-pipeline.md), [../architecture/domain.md](../architecture/domain.md) |

### 7. Record / History

| 항목 | 내용 |
|---|---|
| 시작점 | `record_view`, `geofence_record_view_model`, `notification_view_model` |
| 흐름 | 기록 화면에서 지오펜스/알림/친구 요청 기록 조회 -> 로컬 DB 주기 동기화 -> 앱 resume 시 갱신 -> 유지보수 작업 수행 |
| 서버에서 같이 봐야 할 문서 | [fcm-token-failure-chain.md](fcm-token-failure-chain.md), [friend-request.md](friend-request.md), [friend-block.md](friend-block.md) |

### 8. Setting / My Info / Terms

| 항목 | 내용 |
|---|---|
| 시작점 | `setting_view`, `my_info_view_model`, `terms_list_view` |
| 흐름 | 내 정보 조회/수정 -> 약관 목록과 동의 상태 확인 -> 배터리/권한 설정 섹션 연결 -> 계정 상태 관련 액션 노출 |
| 서버에서 같이 봐야 할 문서 | [oidc-signup-activation.md](oidc-signup-activation.md), [../security/README.md](../security/README.md), [../architecture/domain.md](../architecture/domain.md) |

---

## 정본과의 관계

1. 이 문서는 Flutter 사용 흐름을 빠르게 훑기 위한 인덱스다.
2. 서버 검증 규칙, 예외 코드, 재시도 정책은 개별 flow/security 문서가 정본이다.
3. Flutter 흐름과 서버 흐름이 충돌하면 서버 코드와 서버 문서를 우선한다.
