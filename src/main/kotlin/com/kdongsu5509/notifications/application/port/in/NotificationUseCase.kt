package com.kdongsu5509.notifications.application.port.`in`

import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationStatus
import java.util.UUID

interface NotificationUseCase {

    // --- 발송 요청 ---
    // 대상 수만큼 발송 이벤트를 발행한다. 반드시 트랜잭션 안에서 발행되어야 한다.
    fun request(command: NotificationCommand)

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
