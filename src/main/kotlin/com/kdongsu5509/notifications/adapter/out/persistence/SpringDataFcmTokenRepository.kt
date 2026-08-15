package com.kdongsu5509.notifications.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SpringDataFcmTokenRepository : JpaRepository<FcmTokenJpaEntity, Long> {
    fun findByOwnerId(ownerId: UUID): FcmTokenJpaEntity?
    fun deleteByOwnerId(ownerId: UUID): Long
}
