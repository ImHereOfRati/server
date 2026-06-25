# Flow 문서

ImHere 기능을 **어디서 시작해서 어디로 끝나는지** 기준으로 읽는 문서 모음입니다.  
서버 내부 시퀀스와 앱 단 작업 흐름을 같이 보되, 서버 정본은 각 개별 flow 문서와 코드입니다.

---

## 핵심 판단 / 흐름 구분

| 구분 | 내용 | 문서 |
|---|---|---|
| 서버 시퀀스 | 인증/회원, 친구, 알림을 서버 처리 순서로 설명 | 아래 개별 flow 문서 |
| 앱 실전 흐름 | 화면/작업 단위로 "어디서 시작해서 어디서 끝나는지"를 설명 | [practical-feature-flows.md](practical-feature-flows.md) |
| 데이터 구조 | 모든 흐름이 기대하는 테이블/상태 정의 | [../infra/db-schema.md](../infra/db-schema.md) |

---

## 문서 지도

### 인증 / 회원

| 순서 | 문서 | 범위 |
|---|---|---|
| 1 | [oidc-login.md](oidc-login.md) | 기존 사용자 로그인 |
| 2 | [oidc-signup-activation.md](oidc-signup-activation.md) | 신규 가입, PENDING -> ACTIVE |
| 3 | [token-refresh.md](token-refresh.md) | Refresh Token 재발급 |
| 4 | [admin-ott.md](admin-ott.md) | 관리자 OTT 로그인 |

### 친구

| 순서 | 문서 | 범위 |
|---|---|---|
| 5 | [friend-request.md](friend-request.md) | 요청, 수락, 거절 |
| 6 | [friend-block.md](friend-block.md) | 차단, 제한, 해제 |

### 알림

| 순서 | 문서 | 범위 |
|---|---|---|
| 7 | [notification-pipeline.md](notification-pipeline.md) | MQ In -> Out 처리 |
| 8 | [rabbitmq-dlq-replay.md](rabbitmq-dlq-replay.md) | 재시도, DLQ, Admin Replay |
| 9 | [fcm-token-failure-chain.md](fcm-token-failure-chain.md) | FCM 토큰 등록, 실패 체인 |

### 앱 실전 흐름

| 순서 | 문서 | 범위 |
|---|---|---|
| 10 | [practical-feature-flows.md](practical-feature-flows.md) | Auth, Permission, Geofence, FCM, Friend, Record, Setting |

---

## 자주 참조하는 항목

* 인증 설정 값: [../security/README.md](../security/README.md)
* 데이터 구조: [../infra/db-schema.md](../infra/db-schema.md)
* 앱 실전 흐름에서 서버 정본을 확인할 때: 각 개별 flow 문서
