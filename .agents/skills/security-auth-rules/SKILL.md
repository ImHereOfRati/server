---
name: security-auth-rules
description: Use when changing authentication, authorization, Spring Security context access, or auth-related exception handling.
---

# Security & Authentication Rules

## Rule 1 — Extracting Authenticated User

현재 로그인한 사용자의 정보를 컨트롤러에서 가져올 때는 `SecurityContextHolder`를 직접 접근하지 마세요.
대신 Controller의 메서드 파라미터로 `@AuthenticationPrincipal` 어노테이션과 커스텀 UserDetails인 `ImHereUserDetails`를 주입받아 사용합니다.

```kotlin
// ✅ 권장
@GetMapping("/my-profile")
fun getMyProfile(
    @AuthenticationPrincipal user: ImHereUserDetails
): MemberResponse {
    val email = user.username // 로그인한 유저 식별자
    // ...
}
```

## Rule 2 — Authorization & Exceptions

사용자 인증(Authentication)이 되지 않은 상태이거나 접근 권한(Authorization)이 부족한 비즈니스 로직에 도달했을 때,
일반적인 `RuntimeException`이 아닌 도메인별 예외 Enum에 정의된 에러 코드의 `.throwIt()` 확장 함수를 호출해야 합니다.

* 권한 부족 시: HTTP 403을 반환하는 에러코드 정의 후 `.throwIt()` 호출
* 인증 정보 누락/만료 시: HTTP 401을 반환하는 에러코드 정의 후 `.throwIt()` 호출

## Rule 3 — Public Auth Paths in Tests

Test profile whitelist must match the real public auth endpoints used by integration tests.
If `/api/auth/**` is protected while tests hit those URLs, the request will fail in the security filter chain before controller logic runs.

## Rule 4 — Mixed Authentication for Admin APIs (Session + JWT)

어드민용 REST API(`/api/admin/**`)와 어드민 뷰 화면(`/admin/**`)이 공존할 때, 화면상에서 AJAX/Fetch 요청을 보내기 위해 세션 쿠키 기반의 인증이 필요함과 동시에 백엔드 통합 테스트 및 외부 연동 클라이언트에서 JWT Bearer 토큰 인증을 함께 수행해야 할 경우가 있습니다.

이 경우 다음과 같은 전략을 사용합니다:
1. **필터 체인 분리**: `/api/admin/**` 경로를 처리하는 전용 `SecurityFilterChain`을 `@Order(1)` 등 우선순위 높게 배치합니다.
2. **세션/JWT 동시 적용**: `sessionCreationPolicy = SessionCreationPolicy.IF_REQUIRED`를 통해 기존 어드민 세션을 활용하도록 하고, `addFilterBefore(jwtAuthenticationFilter())`를 추가하여 JWT 토큰을 실어 보낸 요청도 함께 파싱되도록 합니다.
3. **예외 처리 커스텀**: 해당 API 필터 체인에서는 unauthenticated 접근 시 어드민 로그인 페이지로 302 리다이렉트되지 않도록 `authenticationEntryPoint`를 커스텀하여 `401 Unauthorized` 또는 전역 API 에러 응답 포맷을 반환하도록 재정의합니다.
