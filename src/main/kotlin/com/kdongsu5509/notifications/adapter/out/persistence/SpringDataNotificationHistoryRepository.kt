package com.kdongsu5509.notifications.adapter.out.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataNotificationHistoryRepository : JpaRepository<NotificationHistoryJpaEntity, Long> {
    fun findByReceiverEmailOrderByCreatedAtDesc(
        receiverEmail: String,
        pageable: Pageable
    ): Page<NotificationHistoryJpaEntity>
}
