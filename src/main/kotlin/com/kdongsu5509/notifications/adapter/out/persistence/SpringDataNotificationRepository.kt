package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface SpringDataNotificationRepository : JpaRepository<NotificationJpaEntity, Long> {
    @Modifying
    @Query("update NotificationJpaEntity n set n.status = :pending where n.status = :processing and n.updatedAt < :before")
    fun recoverStalled(
        @Param("pending") pending: NotificationStatus,
        @Param("processing") processing: NotificationStatus,
        @Param("before") before: LocalDateTime,
    ): Int

    @Modifying
    @Query("update NotificationJpaEntity n set n.status = :processing where n.id = :id and n.status in :sendable")
    fun claimForDelivery(
        @Param("id") id: Long,
        @Param("processing") processing: NotificationStatus,
        @Param("sendable") sendable: Collection<NotificationStatus>,
    ): Int

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

    fun deleteByTargetIdentifier(targetIdentifier: String): Long

}
