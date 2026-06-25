# Kotlin Conventions

이 문서는 Notion 원칙과 실제 코드에서 반복되는 패턴을 합쳐 정리합니다. 충돌 시 코드가 우선입니다.

---

## 핵심 판단

| 결정 | 내용 | 근거 |
|---|---|---|
| `val` 우선 | 상태 변경을 최소화 | 저장소 전반 |
| null 처리와 예외를 한 줄에서 종료 | `?: throwIt()` 패턴 사용 | `LoginService.kt:27` |
| 설정 주입 두 방식 공존 | `@param:Value`, `@ConfigurationProperties` 둘 다 사용 | `ImHereOttSuccessHandler.kt:17`, `ImHereJwtProperties.kt:7` |
| 문맥 기반 테스트 네이밍 | Notion의 단일 규칙보다 실제 파일 관례를 우선 | `LoginServiceTest.kt:30` |

---

## 기본 규칙

* `val` 우선, `var` 최소화
* `!!` 금지
* 생성자 주입 사용
* 설정값 주입은 `@param:Value` 또는 `@ConfigurationProperties`

---

## 예외 처리 패턴

`IllegalStateException`보다 도메인 에러 코드 + `throwIt()`를 우선합니다.

```kotlin
val token = cachePort.find(key, String::class.java)
    ?: AuthException.IMHERE_KEY_NOT_FOUND_IN_CACHE.throwIt()
```

이 패턴은 null 처리와 예외 발생을 한 줄에서 끝내기 때문에 서비스 흐름을 읽기 쉽습니다.

---

## 트랜잭션 관례

| 경우 | 관례 |
|---|---|
| 조회 서비스 | `@Transactional(readOnly = true)` 우선 |
| 쓰기 유스케이스 | 메서드 수준 `@Transactional` |

---

## 패키지 / 모듈 관례

* 외부 의존성이 큰 모듈은 포트/어댑터 분리
* 단순 CRUD는 MVC 유지
* JPA Entity와 도메인 모델을 분리하는 모듈은 Mapper 사용

---

## 테스트 네이밍 관찰

실제 저장소에는 두 패턴이 공존합니다.

* `login_success` 같은 snake_case 스타일
* `@DisplayName`으로 한국어 설명 추가

즉, Notion에 적힌 단일 CamelCase 규칙만으로 현재 저장소를 설명하면 부정확합니다. 새 테스트는 파일 문맥을 우선 따라야 합니다.

---

## 코드 스멜 힌트

* private 메서드가 과도하게 늘어나면 책임 분리를 의심합니다.
* 호출 순서 의존이 강하면 서비스 분해 또는 도메인 메서드 이동을 검토합니다.

---

## 코드 근거

* `@param:Value` 사용 예: `src/main/kotlin/com/kdongsu5509/auth/security/handler/ImHereOttSuccessHandler.kt:17`
* `@ConfigurationProperties` 사용 예: `src/main/kotlin/com/kdongsu5509/auth/adapter/out/jwt/ImHereJwtProperties.kt:7`
* `throwIt()` 기반 null 처리 예: `src/main/kotlin/com/kdongsu5509/auth/application/service/LoginService.kt:27`

---

## 관련 문서

* 에러 처리: [error-handling.md](error-handling.md)
* 테스트 구조: [testing.md](testing.md)
