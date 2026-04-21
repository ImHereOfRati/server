package com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto

enum class NotificationType(val pathTemplate: String) {
    FRIEND_REQUEST("/contact/requests"),
    TERMS_UPDATE("/terms-detail/{termId}"),
    LOCATION_SHARE_RECIPIENT("/record/notifications"),
    ARRIVAL_CONFIRMATION("/record/notifications"),
    DELIVERY_RESULT_NOTICE("/record/send-history")
}
