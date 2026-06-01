---
name: jpa-querydsl-rules
description: JPA와 QueryDSL 사용 시 N+1 문제 방지, 동적 쿼리 분리, BaseEntity 상속 등의 규칙을 정의한다.
---

# JPA & QueryDSL Guidelines

## Rule 1 — BaseEntity Inheritance

새로운 엔티티를 생성할 때, 생성일시(`createdAt`)와 수정일시(`updatedAt`)가 필요한 경우 반드시 `com.kdongsu5509.shared.BaseTimeEntity`를 상속해야 합니다.
단순 식별자와 생성일시만 필요하다면 `BaseEntity`를 활용합니다.

```kotlin
@Entity
class Member : BaseTimeEntity() {
    // id, createdAt, updatedAt은 부모 클래스에서 제공됨
}
```

## Rule 2 — QueryDSL for Complex/Dynamic Queries

단순한 CRUD나 조건 쿼리는 Spring Data JPA(`JpaRepository`)를 사용합니다.
다만 2개 이상의 조건이 동적으로 결합되거나, 복잡한 통계/Join 쿼리가 필요한 경우에는 **QueryDSL**을 필수적으로 사용해야 합니다. 
Repository 내부에 Custom 인터페이스와 Impl 클래스를 만들어 적용하여 유지보수성을 높입니다.

## Rule 3 — Prevent N+1 Problem

연관된 엔티티(ManyToOne, OneToMany 등)를 루프 안에서 지연 로딩(Lazy Loading)으로 접근하면 N+1 문제가 발생합니다.
이를 방지하기 위해 데이터를 한 번에 가져와야 하는 경우 반드시 **Fetch Join**을 사용하거나, QueryDSL의 `fetchJoin()` 메서드를 적극 활용하세요.
