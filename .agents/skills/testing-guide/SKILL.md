---
name: testing-guide
description: ImHere Server 프로젝트의 테스트 전략. 단위/슬라이스/통합 테스트 기준, JUnit5/Mockito/AssertJ 사용법을 정의한다.
---

# Testing Guide — ImHere Server (Kotlin/Spring)

## Test Strategy

| Level | Target | Tool & Annotation |
|-------|--------|-------------------|
| **Unit Test** | 도메인 로직, 독립적 서비스 | `JUnit5`, `Mockito`, `AssertJ` |
| **Slice Test** | Controller, Repository | `@WebMvcTest`, `@DataJpaTest` |
| **Integration Test** | Controller부터 DB까지 E2E 플로우 | `@SpringBootTest`, `MockMvc` |

## Pre-task Checklist

- [ ] 단위 테스트가 무의미한 환경(예: DB 접근 위주)이면 **Slice Test** 작성
- [ ] 모든 API 엔드포인트(Controller)에 대해 **Integration Test** 필수 작성
- [ ] Assertion 시 `AssertJ`를 활용하여 가독성 높은 검증 수행

## Rule 1 — Controller Slice Test (@WebMvcTest)

컨트롤러 슬라이스 테스트는 **인증/인가에 얽매이지 않고** 다음 항목들을 검증하는 데 집중해야 합니다:
1. **Jackson Parsing:** 요청/응답 DTO의 JSON 직렬화 및 역직렬화
2. **정상 요청 처리:** 예상된 파라미터와 바디가 올바르게 컨트롤러에 매핑되는지 확인
3. **잘못된 요청 핸들링:** `@Valid` 등을 통한 유효성 검사 실패 시(Bad Request)의 에러 핸들링

*외부 의존성은 `@MockBean`으로 모킹하되, 로직의 일부를 실제 구현체로 동작시켜야 하는 경우에는 `@SpyBean` (또는 `@Spy`)을 사용하세요.*

```kotlin
// ✅ Controller Slice Test (파싱 및 Validation 집중 검증)
@WebMvcTest(MemberController::class)
class MemberControllerSliceTest {
    @Autowired lateinit var mockMvc: MockMvc
    
    @MockBean lateinit var memberService: MemberService
    
    // 일부 객체의 실제 로직 실행이 필요할 경우 Spy 활용
    // @SpyBean lateinit var someMapper: SomeMapper 
    
    @Test
    fun `잘못된 이메일 형식으로 요청 시 400 에러를 반환한다`() { ... }
}
```

## Rule 2 — Controller Integration Test (@SpringBootTest)

컨트롤러 통합 테스트는 실제 Spring Context를 띄워 **실제 인증/인가(Security) 기반**으로 작동해야 합니다.
통합 테스트에서는 인증 및 권한이 부여된 상태에서의 정상 요청뿐만 아니라, 잘못된 보안 요청이나 비즈니스 에러 상황에 대한 E2E 플로우를 검증합니다.
또한, **이 통합 테스트를 실행하면서 RestDocs + epages를 활용해 API 문서(정상 및 에러 응답 모두)를 자동 생성**해야 합니다 (`api-docs-rules` 참고).

```kotlin
// ✅ Controller Integration Test (Security 플로우 검증 및 RestDocs 문서화)
@SpringBootTest
@AutoConfigureMockMvc
class MemberControllerIntegrationTest {
    @Autowired lateinit var mockMvc: MockMvc
    
    @Test
    fun `인증된 사용자의 정상 회원가입 요청 시 200 OK 및 문서화`() {
        mockMvc.perform(
            post("/api/members")
                .header("Authorization", "Bearer valid_token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "test@test.com", "password": "..."}""")
        ).andExpect(status().isOk)
         .andDo(MockMvcRestDocumentationWrapper.document("member-create-success", ...))
    }

    @Test
    fun `인증되지 않은 사용자가 접근 시 401 Unauthorized 및 에러 문서화`() {
        mockMvc.perform(get("/api/members/my"))
            .andExpect(status().isUnauthorized)
            .andDo(MockMvcRestDocumentationWrapper.document("member-get-unauthorized", ...))
    }
}
```

## Rule 3 — Repository Slice Test (@DataJpaTest)

JPA 쿼리나 영속성 컨텍스트를 검증해야 하는 경우 Repository 슬라이스 테스트를 수행합니다.

```kotlin
@DataJpaTest
class MemberRepositoryTest {
    @Autowired lateinit var repository: MemberRepository

    @Test
    fun `이름으로 회원 조회`() { ... }
}
```
## Rule 4 — Shared Web Integration Harness

공통 웹 통합 테스트 초기화는 `WebIntegrationTestSupport` 같은 베이스 클래스에 모은다.
개별 컨트롤러 통합 테스트는 `@MockitoBean`, 도메인 fixture, 요청/응답 검증만 선언한다.
## Rule 5 — 401 Documentation Without Body

When a security filter returns an empty 401 response, do not attach `responseFields(...)` in RestDocs.
Document only the status, or use a response body only when the filter/controller actually writes one.

## Rule 6 — Separation of Validation and Business Error Testing

- **요청 값 검증 (Validation Error)**: 파라미터 누락, 형식 오류 등 `@Valid`에 의한 400 Bad Request 검증은 슬라이스 테스트(`@WebMvcTest`) 계층에서 수행합니다.
- **비즈니스/도메인 에러 (Business Error)**: 도메인 로직이나 비즈니스 정책 위반으로 발생하는 에러(예: 필수 약관 미동의, 중복 가입, 잘못된 상태에서의 요청 등)는 통합 테스트(`@SpringBootTest`) 계층에서 E2E로 검증합니다.
- **문서화 원칙**: 통합 테스트에서 발생하는 비즈니스 에러는 **반드시 `RestDocs`를 통해 문서화**하여 클라이언트에게 도메인 제약 사항을 명확히 전달해야 합니다. 반면, 단순 파라미터 검증 실패는 문서화를 생략하거나 가볍게 다룰 수 있습니다.
