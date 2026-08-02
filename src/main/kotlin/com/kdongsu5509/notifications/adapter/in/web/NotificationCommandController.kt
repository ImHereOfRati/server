package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.notifications.adapter.`in`.web.dto.NotificationRequest
import com.kdongsu5509.notifications.application.port.`in`.NotificationUseCase
import com.kdongsu5509.shared.response.ApiResponse
import org.springframework.http.HttpStatus.ACCEPTED
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications", version = "1")
class NotificationCommandController(
    private val notificationUseCase: NotificationUseCase,
) {

    companion object {
        const val SUCCESS_MSG = "알림이 발송 큐에 등록되었습니다."
    }

    @ResponseStatus(ACCEPTED)
    @PostMapping
    fun send(
        @AuthenticationPrincipal user: ImHereUserDetails,
        @Validated @RequestBody request: NotificationRequest
    ): ApiResponse<String> {
        notificationUseCase.request(
            request.toCommand(user.nickname, user.requiredUserId)
        )

        return ApiResponse.success(SUCCESS_MSG)
    }
}
