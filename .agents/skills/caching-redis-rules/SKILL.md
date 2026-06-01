---
name: caching-redis-rules
description: Redis 및 Spring Cache를 활용한 성능 최적화, Cache Key 네이밍, TTL 적용, 데이터 불일치 방지 규칙을 정의한다.
---

# Caching & Redis Rules

## Rule 1 — Target for Caching

캐싱은 **"조회가 매우 빈번하지만, 데이터의 변경이 자주 일어나지 않는"** 데이터에만 적용합니다.
(예: 공지사항, 기본 설정 정보, 고정된 약관 등). 변경이 잦은 데이터(예: 실시간 포인트, 진행 중인 상태값)에 캐시를 적용하면 데이터 불일치(Stale Data) 문제가 발생하므로 적용을 금지합니다.

## Rule 2 — Cache Key Naming Convention

Redis 캐시 키는 목적과 식별자를 명확히 구분할 수 있도록 작성해야 합니다. 주로 콜론(`:`)을 사용하여 계층을 분리합니다.

* 형식: `domain:type:identifier`
* 예시: `user:profile:123`, `term:active_list`

Spring `@Cacheable` 사용 시 `value`(캐시 이름)와 `key` 속성을 명확히 지정하세요.

```kotlin
// ✅ 권장
@Cacheable(value = ["userProfile"], key = "#userId")
fun getUserProfile(userId: Long): UserProfile { ... }
```

## Rule 3 — Cache Eviction & TTL

* **TTL(Time To Live):** 영원히 유지되는 캐시는 메모리 누수와 치명적인 데이터 불일치를 초래합니다. Redis 설정(`RedisCacheConfiguration`)에서 데이터의 특성에 맞는 적절한 만료 시간을 설정해야 합니다.
* **Cache Evict:** 데이터가 수정되거나 삭제되는 로직에는 반드시 `@CacheEvict`를 선언하여 기존 캐시를 즉각적으로 무효화해야 합니다.

```kotlin
// 데이터 업데이트 시 해당 키의 캐시 삭제
@CacheEvict(value = ["userProfile"], key = "#userId")
fun updateProfile(userId: Long, req: UpdateReq) { ... }
```
