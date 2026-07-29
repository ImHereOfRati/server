package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface SpringDataNotificationRepository : JpaRepository<NotificationJpaEntity, Long> {
    fun findByDedupeKey(dedupeKey: String): NotificationJpaEntity?
    fun countByDedupeKey(dedupeKey: String): Long

    fun findByStatus(status: NotificationStatus, pageable: Pageable): Page<NotificationJpaEntity>

    fun findByTargetIdentifierAndMethodAndStatus(
        targetIdentifier: String,
        method: NotificationMethod,
        status: NotificationStatus,
        pageable: Pageable,
    ): Page<NotificationJpaEntity>

    fun findByStatusInAndUpdatedAtBefore(
        statuses: Collection<NotificationStatus>,
        updatedAt: LocalDateTime,
        pageable: Pageable,
    ): List<NotificationJpaEntity>
}
