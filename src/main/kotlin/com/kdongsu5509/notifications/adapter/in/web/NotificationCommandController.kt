package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.notifications.adapter.`in`.web.dto.MultiNotificationRequest
import com.kdongsu5509.notifications.adapter.`in`.web.dto.NotificationRequest
import com.kdongsu5509.notifications.application.port.`in`.NotificationEnqueueUseCase
import com.kdongsu5509.shared.response.ApiResponse
import org.springframework.http.HttpStatus.ACCEPTED
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications", version = "1")
class NotificationCommandController(
    private val notificationEnqueueUseCase: NotificationEnqueueUseCase
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
        val notificationCommand = request.toCommand(
            user.nickname,
            user.username
        )
        notificationEnqueueUseCase.enqueue(notificationCommand)
        return ApiResponse.success(SUCCESS_MSG)
    }

    @ResponseStatus(ACCEPTED)
    @PostMapping("/batch")
    fun sendMultiple(
        @AuthenticationPrincipal user: ImHereUserDetails,
        @Validated @RequestBody request: MultiNotificationRequest
    ): ApiResponse<String> {
        val notificationCommand = request.toCommand(
            user.nickname,
            user.username
        )
        notificationEnqueueUseCase.enqueueMultiple(notificationCommand)
        return ApiResponse.success(SUCCESS_MSG)
    }
}
