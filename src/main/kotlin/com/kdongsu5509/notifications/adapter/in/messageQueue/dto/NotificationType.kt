package com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto

import com.google.firebase.messaging.AndroidConfig

enum class NotificationType(val appPath: String) {
    FRIEND_REQUEST_RECEIVED("/contact/requests"),
    FRIEND_REQUEST_ACCEPTED("/friend/list"),
    LOCATION_SHARE_RECEIVED("/record/notifications"),
    ARRIVAL("/record/notifications"),
    DEPARTURE("/record/notifications"),
    ARRIVAL_CONFIRMATION("/record/notifications"),
    TERMS_UPDATE_NOTICE("/terms-detail/{termId}"),
    DELIVERY_RESULT_NOTICE("/record/send-history"),
    DELIVERY_FAILED_NOTICE("/record/send-history");

    companion object {
        val CLIENT_ALLOWED = setOf(
            LOCATION_SHARE_RECEIVED,
            ARRIVAL,
            DEPARTURE,
            ARRIVAL_CONFIRMATION
        )

        fun fromName(name: String?): NotificationType? =
            name?.let { runCatching { valueOf(it) }.getOrNull() }
    }

    val androidChannelId: String
        get() = when (this) {
            ARRIVAL, ARRIVAL_CONFIRMATION, DEPARTURE -> "fcm_critical_channel"
            FRIEND_REQUEST_RECEIVED, LOCATION_SHARE_RECEIVED -> "fcm_high_channel"
            FRIEND_REQUEST_ACCEPTED, DELIVERY_FAILED_NOTICE -> "fcm_normal_channel"
            TERMS_UPDATE_NOTICE, DELIVERY_RESULT_NOTICE -> "fcm_silent_channel"
        }

    val androidPriority: AndroidConfig.Priority
        get() = when (this) {
            TERMS_UPDATE_NOTICE, DELIVERY_RESULT_NOTICE -> AndroidConfig.Priority.NORMAL
            else -> AndroidConfig.Priority.HIGH
        }
}
