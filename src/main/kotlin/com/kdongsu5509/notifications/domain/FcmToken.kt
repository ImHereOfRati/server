package com.kdongsu5509.notifications.domain

import java.time.LocalDateTime

data class FcmToken(
    val id: Long? = null,
    val email: String,
    val fcmToken: String,
    val deviceType: DeviceType,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
) {
    fun update(fcmToken: String) = copy(
        fcmToken = fcmToken,
    )
}
