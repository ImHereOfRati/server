---
name: api-design
description: REST API 엔드포인트 설계 규칙, 응답 공통 포맷, Controller 작성 원칙을 정의한다.
---

# API Design & Controller Rules

## REST API Design Rules

- **Resource-based URIs:** 동사 대신 명사 사용 (예: `/api/members` ✅, `/api/getMembers` ❌)
- **Plural Nouns:** 컬렉션은 복수형 사용 (`/members`, `/orders`)
- **HTTP Methods:** 목적에 맞게 분리 (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`)
- **Versioning:** URI 또는 Header에 API 버전 명시 (예: `/api/v1/members`)

## Controller Guidelines

1. **Thin Controller:** Controller는 HTTP 요청 수신, 파라미터 검증, 응답 포맷팅만 담당합니다. 핵심 비즈니스 로직은 반드시 Service로 위임합니다.
2. **No Manual ApiResponse Wrapping:** 프로젝트 내에 `GlobalResponseHandler`가 구현되어 있으므로, 컨트롤러 메서드 반환 타입으로 `ApiResponse<T>`를 직접 명시하거나 감쌀 필요가 없습니다. 순수 DTO를 반환하면 자동으로 래핑됩니다.

```kotlin
// ✅ 순수 DTO 반환 (GlobalResponseHandler가 자동 래핑)
@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val memberService: MemberService
) {
    @PostMapping
    fun createMember(@RequestBody @Valid req: MemberCreateRequest): MemberResponse {
        val member = memberService.create(req.toCommand())
        return MemberResponse.from(member) // ApiResponse로 감싸지 않음!
    }
}
```
