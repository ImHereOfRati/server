package com.kdongsu5509.notifications.application.dto

import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType
import java.util.*

data class NotificationCommand(
    val senderNickname: String,
    val senderId: UUID,
    val notificationMethod: NotificationMethod,
    val targetIdentifiers: List<String>,
    val type: NotificationType,
    val extraData: Map<String, String> = emptyMap(),
) {
    val body: String?
        get() = extraData[BODY_KEY]?.takeIf(String::isNotBlank)

    companion object {
        const val BODY_KEY = "body"
    }
}
