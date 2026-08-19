package com.kdongsu5509.notifications.domain

enum class NotificationStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED,
    UNKNOWN,
    DEAD;

    fun isSendable(): Boolean = this == PENDING || this == FAILED
}

