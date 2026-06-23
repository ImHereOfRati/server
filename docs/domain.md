# Domain

ImHere의 실제 비즈니스 규칙을 코드/DB 기준으로 정리한다. 엔티티 구조는 [db-schema.md](./db-schema.md), 흐름도는 [flows.md](./flows.md), 엔드포인트는 [api-spec.md](./api-spec.md) 참고.

## Auth — 가입/활성화

- 자체 회원가입이 없다. Kakao/Google OIDC ID Token 검증으로만 계정이 생성된다.
- 신규 사용자는 `PENDING`으로 생성된다 → 약관 동의 등 활성화 절차를 거쳐야 `ACTIVE`가 된다(`/api/auth/activation`).
- `BLOCKED`/`WITHDRAWN` 상태는 로그인 자체가 거부된다.
- 같은 이메일이라도 Provider가 다르면 `oidc_subject`(OIDC `sub` 클레임)가 달라 별도로 저장한다.
- `/api/auth/login`은 신규 유저를 만들지 않는다 — 미가입이면 `USER_NOT_REGISTER`로 거부하고 가입 플로우로 보낸다.

## Friends — 친구 관계

- 친구 요청(`friend_request`)이 수락되면 `friend_relationships`에 **양방향 2행**이 삽입된다(owner=A/friend=B, owner=B/friend=A). 그래서 친구 목록 조회 시 JOIN 없이 `owner_user_id` 기준으로 바로 조회된다.
- 차단/거절은 `friend_restrictions`에 기록되며 타입별로 만료 정책이 다르다:
  - `REJECT`(거절) → 30일 후 자동 만료, 재요청 가능해진다.
  - `BLOCK`(차단) → `expired_at`이 null, 사용자가 직접 해제하기 전까지 영구 유지.
- 요청자/수신자 둘 중 한쪽이라도 상대를 제한(차단/거절)했으면 새 친구 요청이 거부된다(`FRIEND_REQUEST_UNPROCESSABLE_BY_ME` / `_BY_TARGET`).
- 자기 자신에게 요청은 도메인 단에서 막는다(`SELF_FRIENDSHIP`).

## Notifications — 알림

- 알림은 동기 호출이 아니라 RabbitMQ 메시지(`imhere.noti.topic`)로 발행되고, Consumer가 FCM/SMS를 호출한다.
- 메시지 타입은 8종(`NotificationType`): 친구 요청 수신/수락, 위치 공유 수신, 도착/출발, 도착 확인, 약관 개정, 발송 결과 통지 — 각 타입마다 앱 내 딥링크 경로(`appPath`)가 고정되어 있다.
- 동일 메시지 중복 처리를 막기 위해 Redis에 `messageId`를 48시간 TTL로 저장한다(`MessageIdempotencyService`).
- FCM이 `UNREGISTERED`를 반환하면 해당 디바이스 토큰을 즉시 DB에서 삭제한다 — 죽은 토큰이 쌓이지 않는다.
- 큐 처리에 실패하면 지수 백오프로 최대 3회 재시도하고, 그래도 실패하면 DLQ에 적재된다(관리자가 `/api/admin/dead-letter-queues`로 조회/재처리).

## Terms — 약관

- 약관은 타입(`SERVICE`/`PRIVACY`/`LOCATION`/`MARKETING`)별로 버전 관리된다(`uk_terms_type_version`).
- 사용자의 동의 이력은 `user_agreement`에 누적 저장된다 — 새 버전이 시행되면 사용자는 다음 로그인 시 다시 동의해야 하고, 동의할 때마다 신규 행이 쌓인다(과거 동의 기록은 지우지 않는다).

## 의도적으로 서버에 없는 것

- **위치/지오펜스 도메인.** 진입·이탈 판정, 반경, 반복 조건은 모두 Flutter 앱 로컬(SQLite)에서 관리된다. 서버는 "어떤 친구에게 알릴지"와 발송 결과만 안다.
- **알림 읽음 동기화 외 위치 이력.** 서버 DB에는 위치 좌표 자체를 저장하는 테이블이 없다(`notification_history`는 알림 텍스트만 가진다).
