# Internal Architecture

패키지 구조와 모듈별 설계 스타일을 정리한 문서다. 이 프로젝트는 전면적인 단일 아키텍처보다, 기능 성격에 따라 다른 구조를 섞어 쓰는 쪽에 가깝다.

---

## 핵심 판단

| 판단 | 내용 | 근거 |
|---|---|---|
| 모듈별로 구조를 다르게 둔다 | `auth`, `notifications` 는 강한 계층 분리, `user`, `terms` 는 단순 MVC 에 가깝다 | 복잡도와 외부 의존성 수준이 다르다 |
| 외부 연동이 많은 영역만 포트/어댑터를 강화 | OIDC, JWT, MQ, FCM 연동부는 adapter/port 분리를 쓴다 | 테스트 격리와 교체 가능성이 필요하다 |
| 단순 CRUD 는 과설계하지 않는다 | 사용자/약관 일부는 `Controller -> Service -> Repository` 흐름을 유지한다 | 오히려 가독성이 좋아진다 |

---

## 최상위 패키지

```text
src/main/kotlin/com/kdongsu5509/
├── auth
├── friends
├── notifications
├── terms
├── user
├── shared
└── support
```

---

## 모듈별 스타일

| 모듈 | 스타일 | 특징 |
|---|---|---|
| `auth` | 강한 헥사고날 성향 | `adapter`, `application`, `domain`, `port` 분리가 분명하다 |
| `notifications` | 강한 헥사고날 성향 | MQ, FCM, persistence, admin replay 가 포트 경계를 많이 쓴다 |
| `friends` | 절충형 | 서비스는 분리되어 있지만 일부는 전통적 repository 중심이다 |
| `user`, `terms` | MVC 중심 | 단순 조회/수정 흐름이 많고 포트 추상화가 상대적으로 약하다 |
| `shared`, `support` | 공통 기반 | 응답 포맷, 예외 처리, 설정, 외부 SDK 보조 코드가 모인다 |

---

## 공통 모듈 역할

### `shared`

- `ApiResponse<T>`
- 공통 DTO
- `BaseEntity`, `BaseTimeEntity`

### `support`

- 전역 예외 처리
- Spring 설정
- RabbitMQ 설정
- Discord, Firebase, Solapi 등 외부 연동 보조 코드

---

## 경계가 드러나는 지점

1. `auth` 는 로그인 검증, 토큰 발급, 보안 필터가 하나의 보안 축으로 묶인다.
2. `notifications` 는 HTTP API 와 MQ consumer 가 같은 도메인 규칙을 공유한다.
3. `friends` 는 요청/관계/제한이 분리돼 있지만 하나의 사용자 상호작용 기능군으로 움직인다.
4. `support` 는 인프라 코드가 아니라 애플리케이션 전체를 받치는 기술 공용층에 가깝다.

---

## 코드 기준점

- `src/main/kotlin/com/kdongsu5509/auth/`
- `src/main/kotlin/com/kdongsu5509/friends/`
- `src/main/kotlin/com/kdongsu5509/notifications/`
- `src/main/kotlin/com/kdongsu5509/support/handler/GlobalExceptionHandler.kt`

---

## 연관 문서

- [architecture.md](architecture.md)
- [domain.md](domain.md)
- [../conventions/error-handling.md](../conventions/error-handling.md)
- [../conventions/kotlin-conventions.md](../conventions/kotlin-conventions.md)
