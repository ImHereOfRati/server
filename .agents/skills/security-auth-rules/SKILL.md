---
name: security-auth-rules
description: Spring Security 기반 인증 사용자 정보 획득 방법, 인가 예외 처리 등을 정의한다.
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
