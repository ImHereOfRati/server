package com.kdongsu5509.notifications.domain

enum class NotificationType {
    FRIEND_REQUEST_RECEIVED,

    FRIEND_REQUEST_ACCEPTED,

    LOCATION_TARGET,

    ARRIVAL,

    DEPARTURE,

    TERMS_UPDATE_NOTICE,

    DELIVERY_RESULT_NOTICE,

    DELIVERY_FAILED_NOTICE,
    ;

    val isMeta: Boolean
        get() = this == DELIVERY_RESULT_NOTICE || this == DELIVERY_FAILED_NOTICE

    companion object {
        val CLIENT_ALLOWED = setOf(
            LOCATION_TARGET,
            ARRIVAL,
            DEPARTURE,
        )
    }
}
