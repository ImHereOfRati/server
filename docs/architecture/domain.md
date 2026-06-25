# Domain

ImHere 서버가 실제로 다루는 도메인 규칙만 추려서 정리한 문서다. 개념 설명보다, 현재 코드가 어떤 상태 전이와 제약을 강제하는지가 기준이다.

---

## 핵심 판단

| 판단 | 내용 | 근거 |
|---|---|---|
| 로그인과 회원 활성화를 분리 | OIDC 검증만 끝난 사용자는 `PENDING`, 약관 동의 후 `ACTIVE` 로 전환한다 | 약관 동의 전에는 완전 회원으로 취급하지 않는다 |
| 친구 관계는 요청과 관계를 분리 | `FriendRequest` 와 `Friendship` 을 나눠 저장한다 | 요청 중 상태와 성립된 관계를 다르게 다뤄야 한다 |
| 차단은 제한 정보로 관리 | 거절은 `REJECT`, 차단은 `BLOCK` 제한으로 표현한다 | 재요청 금지와 관계 종료를 함께 표현하려는 의도다 |
| 알림은 전송 결과까지 도메인화 | FCM 전송 자체보다 enqueue, dedup, retry, DLQ, 이력 저장까지 포함한다 | 사용자 경험과 운영 복구가 알림 도메인의 일부다 |

---

## 도메인 축

| 영역 | 책임 | 대표 코드 |
|---|---|---|
| `auth` | OIDC 로그인, 회원가입, 활성화, JWT, Admin OTT | `LoginService`, `RegisterService`, `ActivateUserService`, `TokenRefreshService` |
| `friends` | 요청, 수락, 거절, 친구 관계, 차단/제한 | `FriendRequestServiceImpl`, `FriendshipServiceImpl`, `FriendRestrictionServiceImpl` |
| `notifications` | FCM 토큰 등록, MQ consumer, 푸시 전송, DLQ replay | `FcmTokenEnrollService`, `FCMNotificationService`, `DlqAdminService` |
| `terms` | 약관 버전, 동의 기록 | `UserAgreementService` 계열 |
| `user` | 프로필, 상태 변경, 탈퇴 | 사용자 관련 application/service 계층 |

---

## 상태 전이

### 회원 상태

| 상태 | 의미 | 전이 조건 |
|---|---|---|
| `PENDING` | OIDC 신원은 확인됐지만 약관 동의 전 | 회원가입 직후 |
| `ACTIVE` | 서비스 사용 가능 | 활성화 API 에서 약관 동의 완료 |
| `WITHDRAWN` / `BLOCKED` 계열 | 재가입/로그인 제한 대상 | 정책 또는 운영 처리 |

### 친구 제약

| 타입 | 의미 | 특성 |
|---|---|---|
| `REJECT` | 특정 요청 거절 기록 | 만료될 수 있다 |
| `BLOCK` | 차단 | 해제 전까지 유지된다 |

### 토큰/메시지 캐시

| 키 | 의미 | 용도 |
|---|---|---|
| `refresh:{email}` | 사용자별 유효 refresh token | refresh 회전 검증 |
| `messageId` | 알림 dedup 키 | 중복 메시지 제거 |

---

## 도메인 규칙

1. OIDC 로그인만으로는 완전 가입이 아니다.
2. 동일 사용자에 대해 동시에 여러 refresh token 을 허용하지 않는다.
3. 친구 수락은 단방향 상태 변경이 아니라 양방향 `Friendship` 생성이다.
4. FCM `UNREGISTERED` 는 재시도 대상이 아니라 토큰 정리 대상이다.
5. 알림 실패 중 일부는 즉시 실패가 아니라 DLQ 와 replay 로 복구할 수 있어야 한다.

---

## 데이터 기준점

ERD 와 테이블 단위 설명은 [../infra/db-schema.md](../infra/db-schema.md)를 기준으로 본다.

---

## 연관 문서

- [architecture.md](architecture.md)
- [internal-architecture.md](internal-architecture.md)
- [../security/jwt.md](../security/jwt.md)
- [../security/oauth.md](../security/oauth.md)
- [../flows/friend-request.md](../flows/friend-request.md)
- [../flows/friend-block.md](../flows/friend-block.md)
