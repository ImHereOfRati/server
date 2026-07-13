package com.kdongsu5509.shared

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy

/**
 * 생성/수정 시각에 더해 생성/수정 주체(감사 주체)까지 기록하는 감사 상위 타입.
 *
 * 시각 감사([createdAt]/[updatedAt])는 [BaseTimeEntity]가 소유하며 이 타입이 상속으로 공유한다
 * (과거 두 클래스에 중복 정의되던 시각 필드를 단일화 — JPA 컬럼/스키마·감사 리스너 동작은 불변).
 */
@MappedSuperclass
abstract class BaseEntity : BaseTimeEntity() {
    @CreatedBy
    @Column(updatable = false)
    var createdBy: String? = null

    @LastModifiedBy
    var updatedBy: String? = null
}
