package com.kdongsu5509.notifications.domain

import java.time.LocalDateTime

data class FcmToken(
    val id: Long?,
    val userEmail: String,
    val fcmToken: String,
    val deviceType: DeviceType,
    val updatedAt: LocalDateTime?
)