package com.kdongsu5509.notifications.application.dto

import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType

data class NotificationCommand(
    val senderNickname: String,
    val senderEmail: String,
    val notificationMethod: NotificationMethod,
    val targetIdentifier: String,
    val type: NotificationType,
    val extraData: Map<String, String> = emptyMap(),
) {
    val body: String?
        get() = extraData[BODY_KEY]?.takeIf(String::isNotBlank)

    companion object {
        const val BODY_KEY = "body"
    }
}
