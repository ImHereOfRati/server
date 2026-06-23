# Test Guideline

## 테스트 메서드 네이밍

- `대상메서드_결과_이유` 스타일을 `camelCase`로 쓴다(Kotlin 함수명 컨벤션 기준).
- 테스트 메서드 이름은 영어로만 작성한다.
- 상세 설명이 필요하면 `@DisplayName`으로 한국어 설명을 덧붙인다.

```kotlin
fun createReservation_WithBookedSlot_ThrowsException() { ... }
fun createReservation_WithAvailableSlot_CreatesReservation() { ... }
fun deleteReservation_WithExistingReservation_DeletesReservation() { ... }
```

## 테스트 종류

| 종류 | 애노테이션 | 대상 |
|---|---|---|
| 단위 테스트 | (없음, 순수 클래스) | 도메인/서비스 비즈니스 로직 |
| 슬라이스 테스트 | `@WebMvcTest` 등 | 컨트롤러 계층 / 레포지토리 계층 |
| 통합 테스트 | `@SpringBootTest` | HTTP 요청부터 응답까지 전체. DB는 Testcontainers, 단순 케이스는 H2 |

## Mockito 스타일

`BDDMockito` 스타일을 쓴다 — 읽는 흐름을 `given-when-then`과 맞춘다.

```kotlin
given(repository.findById(1L)).willReturn(Optional.of(member))
then(repository).should().save(member)
```

`Mock` 사용을 기본으로 하고, `Spy`는 스프링에 의존적인 기능을 테스트할 때만 제한적으로 쓴다.

## 레이어별 테스트 원칙

원칙: **내가 작성한 코드와 우리 서비스 내부 규칙만** 테스트한다 — 스프링이 잘 동작하는지가 아니라 우리 코드가 스프링 위에서 의도한 결과를 내는지를 본다.

- **Domain**: 비즈니스 규칙만 테스트. Spring 의존성이 있으면 안 된다.
- **Service**: 유스케이스 흐름. Mockito 사용 가능, 중요한 트랜잭션/DB 흐름은 통합 테스트로 한 번 더 검증.
- **Repository**: SQL/쿼리/매핑 테스트. Mock 금지 — 실제 DB 또는 Testcontainers/H2.
- **Controller**: HTTP 요청/응답 계약을 MockMvc로 테스트(필수값 누락 400, 권한 없음 403, 성공 201 등).
- **Transaction**: 어노테이션 존재가 아니라 결과를 테스트한다("저장 실패 시 관련 작업도 같이 롤백되는가").
- **External Client**: 외부 API는 성공/실패/타임아웃/재시도를 테스트한다. 외부 서버를 직접 호출하지 않고 MockWebServer/WireMock을 쓴다.

## 실제 도구

JUnit 5, Mockito(`mockito-kotlin`), Spring Test(MockMvc), Testcontainers, JaCoCo(커버리지). CI에서 `TESTCONTAINERS_RYUK_DISABLED=true`로 돌린다 — 자세한 내용은 [deployment.md](./deployment.md#ci).
