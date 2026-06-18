package com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto

enum class NotificationType(val appPath: String) {
    FRIEND_REQUEST_RECEIVED("/contact/requests"),
    FRIEND_REQUEST_ACCEPTED("/friend/list"),
    LOCATION_SHARE_RECEIVED("/record/notifications"),
    ARRIVAL("/record/notifications"),
    DEPARTURE("/record/notifications"),
    ARRIVAL_CONFIRMATION("/record/notifications"),
    TERMS_UPDATE_NOTICE("/terms-detail/{termId}"),
    DELIVERY_RESULT_NOTICE("/record/send-history");

    companion object {
        val CLIENT_ALLOWED = setOf(
            LOCATION_SHARE_RECEIVED,
            ARRIVAL,
            DEPARTURE,
            ARRIVAL_CONFIRMATION
        )
    }
}
