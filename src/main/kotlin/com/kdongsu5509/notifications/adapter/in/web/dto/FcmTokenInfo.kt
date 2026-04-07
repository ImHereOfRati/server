package com.kdongsu5509.notifications.adapter.`in`.web.dto

import com.kdongsu5509.notifications.domain.DeviceType

data class FcmTokenInfo(
    val fcmToken: String,
    val deviceType: DeviceType
) {
    init {
        require(fcmToken.isNotBlank()) { "FCM 토큰은 필수 항목입니다." }
    }
}
