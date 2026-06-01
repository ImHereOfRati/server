---
name: api-docs-rules
description: REST Docs 및 epages 라이브러리를 활용한 API 명세서 자동 생성(OpenAPI 3.0) 규칙을 정의한다.
---

# API Documentation Rules (Spring REST Docs & OpenAPI)

## Rule 1 — Document via Tests (No Swagger Annotations)

Controller 클래스나 DTO에 Swagger 관련 어노테이션(`@Operation`, `@Schema` 등)을 직접 붙여서 프로덕션 코드를 오염시키지 않습니다.
반드시 **컨트롤러 통합/슬라이스 테스트(`MockMvc`)의 결과물을 통해 문서를 자동 생성**해야 합니다.

```kotlin
// ❌ 금지 (프로덕션 코드 오염)
@Operation(summary = "회원 가입")
@PostMapping
fun createMember(...) 
```

## Rule 2 — Using epages for OpenAPI 3.0

일반적인 Spring REST Docs의 `document(...)` 대신 `epages` 라이브러리의 래퍼(예: `MockMvcRestDocumentationWrapper.document`)를 활용하여 테스트를 작성합니다.
테스트가 통과하면 `build.gradle`에 설정된 `openapi3` 태스크를 통해 OpenAPI 3.0 스펙 문서가 자동 생성됩니다.

```kotlin
// ✅ 권장 (테스트 코드에서 API 명세 작성)
mockMvc.perform(
    post("/api/v1/members")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(request))
).andExpect(status().isOk)
 .andDo(
     MockMvcRestDocumentationWrapper.document(
         "member-create", // 문서 식별자
         requestFields(
             fieldWithPath("email").description("이메일 주소"),
             fieldWithPath("password").description("비밀번호")
         ),
         responseFields(
             fieldWithPath("id").description("생성된 회원 ID")
         )
     )
 )
```

## Rule 3 — Syncing Docs

API Request/Response 필드가 변경될 경우 반드시 해당 API의 테스트 코드의 `requestFields`, `responseFields` 설정도 함께 수정하여 문서가 항상 최신 스펙을 반영하도록 유지합니다.

## Rule 4 — Documenting Error Responses

성공적인(200 OK) 응답뿐만 아니라, **예외 상황 및 에러 응답(400, 401, 403, 404, 비즈니스 에러 등)에 대해서도 반드시 문서화**를 수행해야 합니다.
컨트롤러 통합 테스트에서 실패 케이스를 검증할 때 `.andDo(MockMvcRestDocumentationWrapper.document(...))`를 호출하여, 프론트엔드 개발자가 API 문서만 보고도 어떤 에러 코드와 응답 포맷이 내려오는지 명확히 알 수 있게 해야 합니다.

```kotlin
// ✅ 권장 (에러 응답에 대한 OpenAPI 문서화)
mockMvc.perform(get("/api/v1/members/999"))
    .andExpect(status().isNotFound)
    .andDo(
        MockMvcRestDocumentationWrapper.document(
            "member-get-not-found",
            responseFields(
                fieldWithPath("errorCode").description("에러 코드 (예: USER-300)"),
                fieldWithPath("errorMessage").description("에러 상세 메시지")
            )
        )
    )
```
