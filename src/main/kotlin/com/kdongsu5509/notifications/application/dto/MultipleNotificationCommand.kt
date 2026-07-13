package com.kdongsu5509.notifications.application.dto

import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType

data class MultipleNotificationCommand(
    val senderNickname: String,
    val senderEmail: String,
    val notificationMethod: NotificationMethod,
    val targetIdentifiers: List<String>,
    val type: NotificationType,
    val extraData: Map<String, String> = emptyMap()
) {
    /** 본문 기반 알림(SMS 등)의 본문. `extraData["body"]`의 명명된 접근자이며 공백은 null로 간주한다. */
    val body: String? get() = extraData[NotificationCommand.BODY_KEY]?.takeIf { it.isNotBlank() }

    /** 다중 대상을 단건 커맨드 목록으로 전개한다. fan-out의 주인을 이 정보 홀더로 귀속한다. */
    fun toSingles(): List<NotificationCommand> =
        targetIdentifiers.map { targetId ->
            NotificationCommand(
                senderNickname = senderNickname,
                senderEmail = senderEmail,
                notificationMethod = notificationMethod,
                targetIdentifier = targetId,
                type = type,
                extraData = extraData,
            )
        }
}
