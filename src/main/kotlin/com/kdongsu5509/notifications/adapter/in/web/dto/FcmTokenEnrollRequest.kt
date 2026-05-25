package com.kdongsu5509.notifications.adapter.`in`.web.dto

import com.kdongsu5509.notifications.domain.DeviceType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class FcmTokenEnrollRequest(
    @field:NotBlank(message = "FCM 토큰을 입력해 주세요.")
    val fcmToken: String,

    @field:NotNull(message = "디바이스 타입을 입력해 주세요.")
    val deviceType: DeviceType
)
