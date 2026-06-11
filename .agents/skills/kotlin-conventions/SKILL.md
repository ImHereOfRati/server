---
name: kotlin-conventions
description: Use when writing Kotlin code and applying project conventions around immutability, extension functions, null-safety, and lateinit usage.
---

# Kotlin Language Conventions

## Rule 1 — Favor Immutability (`val`)

자바 스타일의 가변 변수(`var`) 사용을 최대한 억제하고, 재할당이 필요 없는 모든 변수와 속성은 **불변(`val`)** 으로 선언해야 합니다. 
컬렉션 또한 가급적 읽기 전용(`List`, `Set`, `Map`) 인터페이스를 반환하고, 상태 변경이 반드시 필요한 내부 로직에서만 `MutableList` 등을 제한적으로 허용합니다.

## Rule 2 — Avoid `lateinit` and `!!`

* `lateinit var`는 의존성 주입이나 테스트 환경 세팅 등 불가피한 상황을 제외하고는 사용하지 않습니다. 프로덕션 코드에서는 생성자를 통해 객체를 안전하게 초기화하세요.
* Null 아님 단언 연산자(`!!`)는 절대 사용해서는 안 됩니다. 런타임 NullPointerException의 주범입니다. 대신 안전한 호출(`?.`)과 엘비스 연산자(`?:`)를 결합하여 명시적으로 예외를 던지거나 기본값을 제공하세요.

## Rule 3 — Extension Functions for Utilities

단순한 데이터 조작이나 반복되는 포맷팅 로직은 헬퍼/유틸리티 클래스(`StringUtils`, `DateUtil` 등)의 정적 메서드로 만들지 말고, **코틀린 확장 함수(Extension Function)** 로 작성하여 가독성을 높입니다.

```kotlin
// ✅ 권장 (확장 함수)
fun String.toMaskedEmail(): String { ... }
val masked = "user@test.com".toMaskedEmail()

// ❌ 금지 (Java 스타일 정적 유틸리티 호출)
val masked = StringUtils.maskEmail("user@test.com")
```

## Rule 4 — Spring `@Value` Annotation Parameter Targeting

* **Primary Constructor Parameters**: 클래스 프로퍼티로 주입받는 생성자 매개변수에는 `@param:Value("\${prop.path}")`와 같이 `@param:` 타겟을 명시하여 필드 주입 경고를 방지합니다.
* **Method/Function Parameters**: `@Bean` 메서드나 일반 함수 매개변수에 `@Value`를 사용할 때는 `@param:` 지시자를 붙이지 않고 직접 `@Value("\${prop.path}")`로 선언해야 합니다. 그렇지 않으면 컴파일 타임 에러가 발생합니다.
* **Safe Defaults**: `@Value`로 주입받는 프로퍼티는 테스트 환경(`application-test.yml` 등)이나 개발 환경에서 설정 누락으로 인해 Context 로드 실패가 발생하는 것을 막기 위해 가급적 기본값(`@Value("\${prop.path:default_value}")`)을 동봉하여 선언합니다.
