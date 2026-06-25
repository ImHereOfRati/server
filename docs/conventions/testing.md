# Testing

현재 저장소의 테스트 구조와 지원 클래스를 기준으로 정리합니다. 이 프로젝트의 테스트는 "프레임워크가 동작하는가"보다 "우리 코드가 의도한 결과를 내는가"를 확인하는 쪽에 가깝습니다.

---

## 핵심 판단

| 결정 | 내용 | 근거 |
|---|---|---|
| Service는 단위 테스트 우선 | 비즈니스 분기와 예외 코드를 빠르게 검증 | `LoginServiceTest.kt` |
| Controller는 계약 테스트 우선 | status, body, 인증/인가를 함께 봄 | `WebMvcTest`, IntegrationTest 계열 |
| MQ/외부연동은 통합 경계 포함 | RabbitMQ container, Firebase/Solapi mock 사용 | `PersistenceTestSupport.kt:16` |

---

## 테스트 종류

### 단위 테스트

* 대상: 도메인, 서비스
* 도구: JUnit 5, Mockito Kotlin
* 스타일: `given/when/then`

예:

* `LoginServiceTest`
* `FriendRequestServiceImplTest`

### 슬라이스 테스트

* 대상: Controller, 일부 repository 매핑
* 도구: `@WebMvcTest`
* 목적: HTTP 계약, validation, 권한 검증

### 통합 테스트

* 대상: 요청부터 DB/MQ까지 이어지는 흐름
* 도구: `@SpringBootTest`
* 기반 클래스:
  * `PersistenceTestSupport`
  * `WebIntegrationTestSupport`

---

## 테스트 지원 코드

### `PersistenceTestSupport`

* `@SpringBootTest`
* `@Transactional`
* `@ActiveProfiles("test")`
* RabbitMQ Testcontainer 사용
* Firebase / Solapi는 MockitoBean으로 대체

### `WebIntegrationTestSupport`

* MockMvc 구성
* Spring Security 적용
* REST Docs 적용
* UTF-8 필터 적용

---

## 현재 저장소 관례

* Mockito는 BDD 스타일(`given`, `then`)을 사용합니다.
* Controller 테스트는 status, 응답 body, 인증 실패를 함께 봅니다.
* Repository 테스트는 mock보다 실제 DB/컨테이너 기반 검증을 선호합니다.
* 테스트 이름은 영어 메서드명 + 한국어 `@DisplayName` 조합이 많습니다.

실제 예시:

```kotlin
@Test
@DisplayName("등록된 사용자가 OIDC 토큰으로 로그인을 시도하면 JWT 토큰을 발급한다")
fun login_success() { ... }
```

---

## 권장 검증 포인트

| 계층 | 무엇을 검증하는가 |
|---|---|
| Service | 도메인 분기, 예외 코드 |
| Controller | HTTP status, 응답 JSON, 인증/인가 |
| Repository | 쿼리, 매핑, 제약 조건 |
| MQ / 외부연동 | 재시도, 실패 전파, 부수효과 |

---

## 코드 근거

* 서비스 단위 테스트 예: `src/test/kotlin/com/kdongsu5509/auth/application/service/LoginServiceTest.kt:30`
* 통합 테스트 베이스: `src/test/kotlin/com/common/testsupport/PersistenceTestSupport.kt:16`
* 웹 통합 테스트 베이스: `src/test/kotlin/com/common/testsupport/WebIntegrationTestSupport.kt:14`

---

## 관련 문서

* 에러 처리: [error-handling.md](error-handling.md)
* 토큰 재발급 시퀀스: [../flows/token-refresh.md](../flows/token-refresh.md)
* 알림 파이프라인: [../flows/notification-pipeline.md](../flows/notification-pipeline.md)
