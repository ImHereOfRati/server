package com.kdongsu5509.notifications.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SpringDataFcmTokenRepository : JpaRepository<FcmTokenJpaEntity, Long> {
    fun findByEmail(email: String): FcmTokenJpaEntity?
}
