package com.kdongsu5509.notifications.application.port.out

import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationStatus
import java.time.LocalDateTime

interface NotificationPersistencePort {
    fun save(notification: Notification): Notification
    fun findById(id: Long): Notification?

    /** 멱등 키로 이미 접수된 발송이 있는지 본다. */
    fun findByDedupeKey(dedupeKey: String): Notification?

    fun findByStatus(status: NotificationStatus, page: Int, size: Int): List<Notification>
    fun findInbox(receiverEmail: String, page: Int, size: Int): List<Notification>
    fun findRecoverable(before: LocalDateTime, limit: Int): List<Notification>
    fun deleteById(id: Long)
}
