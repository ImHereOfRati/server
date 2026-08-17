package com.kdongsu5509.notifications.event

import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.shared.event.DomainEvent
import com.kdongsu5509.support.exception.type.InvalidInputException
import java.time.LocalDateTime
import java.util.*

data class NotificationEvent(
    val senderNickname: String,
    val senderId: UUID,
    val notificationMethod: NotificationMethod,
    val targetIdentifier: String,
    val type: NotificationType,
    val extraData: Map<String, String> = emptyMap(),
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent {
    // SMS는 대상을 전화번호로 지목하므로 사용자로 풀리지 않는다.
    val targetUserId: UUID?
        get() = try {
            UUID.fromString(targetIdentifier)
        } catch (_: IllegalArgumentException) {
            null
        }

    // SMS 본문은 템플릿이 아니라 요청자가 직접 쓴다. FCM은 템플릿이 만든 본문을 그대로 쓴다.
    fun bodyOverride(): String? {
        if (notificationMethod != NotificationMethod.SMS) return null

        return extraData[NotificationCommand.BODY_KEY]
            ?.takeIf(String::isNotBlank)
            ?: throw InvalidInputException("SMS 본문이 누락되었습니다.")
    }

    companion object {
        fun from(command: NotificationCommand): List<NotificationEvent> =
            command.targetIdentifiers.map {
                NotificationEvent(
                    senderNickname = command.senderNickname,
                    senderId = command.senderId,
                    notificationMethod = command.notificationMethod,
                    targetIdentifier = it,
                    type = command.type,
                    extraData = command.extraData,
                )
            }


        fun deliveryReceipt(senderId: UUID, type: NotificationType): NotificationEvent =
            NotificationEvent(
                senderNickname = "ImHere",
                senderId = senderId,
                notificationMethod = NotificationMethod.FCM,
                targetIdentifier = senderId.toString(),
                type = type,
            )
    }
}
