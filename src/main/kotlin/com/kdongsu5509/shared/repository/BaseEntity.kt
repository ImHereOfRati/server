package com.kdongsu5509.shared.repository

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy

@MappedSuperclass
abstract class BaseEntity : BaseTimeEntity() {
    @CreatedBy
    @Column(updatable = false)
    var createdBy: String? = null

    @LastModifiedBy
    var updatedBy: String? = null
}
