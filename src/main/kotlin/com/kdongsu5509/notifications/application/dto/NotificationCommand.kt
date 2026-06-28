package com.kdongsu5509.notifications.application.dto

import com.kdongsu5509.notifications.domain.NotificationMethod

data class NotificationCommand(
    val senderNickname: String,
    val senderEmail: String,
    val notificationMethod: NotificationMethod,
    val targetIdentifier: String,
    val type: String,
    val extraData: Map<String, String> = emptyMap()
) {
    /** 본문 기반 알림(SMS 등)의 본문. `extraData["body"]`의 명명된 접근자이며 공백은 null로 간주한다. */
    val body: String? get() = extraData[BODY_KEY]?.takeIf { it.isNotBlank() }

    companion object {
        const val BODY_KEY = "body"
    }
}
