---
name: oop-principles
description: 객체지향 원칙 (클린 코드, 조영호의 오브젝트). Tell Don't Ask, DTO/Domain 분리, 캡슐화, 단일 책임 원칙 등을 Kotlin/Spring/JPA 기반으로 규정한다.
---

# OOP Principles (Kotlin & Spring)

## Pre-task Checklist

- [ ] **Tell, Don't Ask:** 엔티티의 상태(Getter)를 꺼내어 조건문으로 판단하지 않고, 엔티티에게 메시지를 전달했는가?
- [ ] **Data vs Object:** DTO와 Entity(Domain)를 철저히 분리했는가?
- [ ] **Encapsulation:** 컬렉션 반환 시 불변으로 래핑(`List.toList()`)했는가?
- [ ] **Rich Domain Model:** 비즈니스 로직이 Service가 아닌 Entity 안에 존재하는가?

## Rule 1 — Tell, Don't Ask

상태를 가져와서 외부에서 연산하지 말고, 객체 내부에서 연산하도록 메시지를 보내세요.

```kotlin
// ✅ 객체에 메시지를 보냄 (Entity 내부 메서드)
member.upgradeToVip()

// ❌ 상태를 꺼내어 외부(Service)에서 조건 판단 후 데이터 변경
if (member.point >= 1000) {
    member.role = Role.VIP
}
```

## Rule 2 — DTO / Domain Separation

Entity는 DB 매핑과 핵심 비즈니스 로직만 담당하며, API 스펙에 맞는 DTO(Request/Response)는 별도로 정의합니다.

```kotlin
// ✅ Controller에서는 철저히 DTO 반환
@GetMapping("/{id}")
fun getMember(@PathVariable id: Long): MemberResponse {
    val member = memberService.findById(id)
    return MemberResponse.from(member) // Entity -> DTO 변환
}
```

## Rule 3 — JPA Collection Encapsulation

JPA Entity의 연관관계 컬렉션은 외부에서 직접 수정할 수 없도록 캡슐화해야 합니다.

```kotlin
@Entity
class Order {
    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    private val _items: MutableList<OrderItem> = mutableListOf()

    // 외부에는 불변 리스트로 제공 (Kotlin의 toList()로 방어적 복사/캐스팅)
    val items: List<OrderItem>
        get() = _items.toList()

    // 비즈니스 메서드를 통해서만 추가
    fun addItem(item: OrderItem) {
        _items.add(item)
        item.setOrder(this)
    }
}
```
