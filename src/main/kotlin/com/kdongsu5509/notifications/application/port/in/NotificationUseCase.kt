package com.kdongsu5509.notifications.application.port.`in`

import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationStatus
import java.util.*

interface NotificationUseCase {

    fun requestDelivery(command: NotificationCommand)

    // --- 수신자 ---
    fun findByRecipientId(recipientId: UUID, page: Int, size: Int): List<Notification>
    fun markAsRead(recipientId: UUID, id: Long)

    // --- 운영자 ---
    fun findAll(status: NotificationStatus, page: Int, size: Int): List<Notification>
    fun findById(id: Long): Notification

    fun redeliver(id: Long)

    fun redeliverAll(count: Int?): Int

    fun discard(id: Long)
}
