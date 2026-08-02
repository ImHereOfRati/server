package com.kdongsu5509.notifications.application.port.out

import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationStatus
import java.time.LocalDateTime
import java.util.UUID

interface NotificationPersistencePort {
    fun save(notification: Notification): Notification
    fun findById(id: Long): Notification?

    fun findByDedupeKey(dedupeKey: String): Notification?

    fun findByStatus(status: NotificationStatus, page: Int, size: Int): List<Notification>
    fun findInbox(recipientId: UUID, page: Int, size: Int): List<Notification>
    fun findRecoverable(before: LocalDateTime, limit: Int): List<Notification>
    fun deleteById(id: Long)
}
