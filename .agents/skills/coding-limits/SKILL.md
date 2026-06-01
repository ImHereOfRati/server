---
name: coding-limits
description: ImHere Server 프로젝트의 코딩 제약. @Autowired 필드 주입 금지, 무분별한 @Data 금지, 로깅 규칙 등 객체 지향 및 Spring 제약 사항을 규정한다.
---

# Coding Limits — ImHere Server (Kotlin/Spring)

## Pre-task Checklist

- [ ] 필드 주입(`@Autowired`) 금지 — 생성자 주입 강제
- [ ] 무분별한 `@Data` 사용 금지 — 필요한 것만 명시 (Kotlin의 경우 무분별한 `data class` 사용 주의)
- [ ] `System.out.println` 금지 — `log.info()`, `log.error()` 등 로거 사용
- [ ] JPA Entity에는 NoArgsConstructor(protected) 필수
- [ ] Entity 내 Setter 사용 금지 — 의미 있는 비즈니스 메서드로 상태 변경

## Rule 1 — Dependency Injection

```kotlin
// ✅ 생성자 주입 (Kotlin에서는 주 생성자 활용)
@Service
class MemberService(
    private val memberRepository: MemberRepository
) { 
    // ... 
}

// ❌ 필드 주입
@Service
class MemberService {
    @Autowired
    private lateinit var memberRepository: MemberRepository
}
```

## Rule 2 — Logging

```kotlin
import org.slf4j.LoggerFactory

// ✅ Slf4j 기반 로깅
private val log = LoggerFactory.getLogger(javaClass)

log.info("Request received: id={}", id)

// ❌ 콘솔 출력
println("Request received")
```

## Rule 3 — Lombok / Data Classes

- Kotlin의 `data class`는 DTO/Response에 적극 활용합니다.
- JPA Entity에 `data class` 사용 시 `equals`, `hashCode`, `toString`이 지연 로딩을 예기치 않게 트리거하거나 순환 참조를 일으킬 수 있으므로 일반 `class`를 권장합니다.

## Rule 4 — Exception Handling

- 개별 Controller에서 try-catch를 남용하지 않고 `@RestControllerAdvice`를 통해 전역적으로 일관되게 에러를 처리합니다.
