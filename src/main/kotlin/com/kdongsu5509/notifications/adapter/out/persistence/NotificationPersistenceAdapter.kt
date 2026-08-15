package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationStatus
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.util.UUID

@Component
class NotificationPersistenceAdapter(
    private val repository: SpringDataNotificationRepository,
    private val mapper: NotificationMapper,
) : NotificationPersistencePort {

    override fun save(notification: Notification): Notification =
        mapper.toDomain(repository.saveAndFlush(mapper.toEntity(notification)))

    override fun findById(id: Long): Notification? =
        repository.findById(id).orElse(null)?.let { mapper.toDomain(it) }

    override fun findByDedupeKey(dedupeKey: String): Notification? =
        repository.findByDedupeKey(dedupeKey)?.let { mapper.toDomain(it) }

    override fun findByStatus(status: NotificationStatus, page: Int, size: Int): List<Notification> =
        repository.findByStatus(status, pageRequest(page, size))
            .content
            .map(mapper::toDomain)

    override fun findInbox(recipientId: UUID, page: Int, size: Int): List<Notification> =
        repository.findByTargetIdentifierAndMethodAndStatus(
            recipientId.toString(),
            NotificationMethod.FCM,
            NotificationStatus.SENT,
            pageRequest(page, size),
        ).content.map(mapper::toDomain)

    override fun findRecoverable(before: LocalDateTime, limit: Int): List<Notification> =
        repository.findByStatusInAndUpdatedAtBefore(
            listOf(NotificationStatus.PENDING, NotificationStatus.FAILED),
            before,
            PageRequest.of(
                0,
                limit.coerceIn(1, 100),
                Sort.by(Sort.Direction.ASC, "updatedAt"),
            ),
        ).map(mapper::toDomain)

    override fun deleteByTargetIdentifier(targetIdentifier: String) {
        repository.deleteByTargetIdentifier(targetIdentifier)
    }

    override fun deleteById(id: Long) = repository.deleteById(id)

    private fun pageRequest(page: Int, size: Int): PageRequest =
        PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100), Sort.by(Sort.Direction.DESC, "createdAt"))
}
