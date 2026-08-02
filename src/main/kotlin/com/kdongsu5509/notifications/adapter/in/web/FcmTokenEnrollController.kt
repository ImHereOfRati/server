package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.notifications.adapter.`in`.web.dto.FcmTokenEnrollRequest
import com.kdongsu5509.notifications.application.port.`in`.FcmTokenEnrollUseCase
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/fcm-tokens", version = "1")
class FcmTokenEnrollController(
    private val fcmTokenEnrollUseCase: FcmTokenEnrollUseCase
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun enroll(
        @AuthenticationPrincipal(expression = "userId") requiredUserId: UUID,
        @Validated @RequestBody request: FcmTokenEnrollRequest
    ) = fcmTokenEnrollUseCase.save(
        requiredUserId,
        request.fcmToken,
        request.deviceType
    )
}
