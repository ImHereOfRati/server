package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.notifications.adapter.`in`.web.dto.MultiNotificationRequest
import com.kdongsu5509.notifications.adapter.`in`.web.dto.NotificationRequest
import com.kdongsu5509.notifications.application.port.`in`.NotificationEnqueueUseCase
import com.kdongsu5509.shared.response.ApiResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications", version = "1")
class NotificationCommandController(
    private val notificationEnqueueUseCase: NotificationEnqueueUseCase
) {
    @PostMapping
    fun send(
        @AuthenticationPrincipal user: ImHereUserDetails,
        @Validated @RequestBody request: NotificationRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        notificationEnqueueUseCase.enqueue(request.toCommand(user.nickname, user.username))
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(null, "알림이 발송 큐에 등록되었습니다."))
    }

    @PostMapping("/batch")
    fun sendMultiple(
        @AuthenticationPrincipal user: ImHereUserDetails,
        @Validated @RequestBody request: MultiNotificationRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        notificationEnqueueUseCase.enqueueMultiple(request.toCommand(user.nickname, user.username))
        return ResponseEntity
            .status(HttpStatus.ACCEPTED)
            .body(ApiResponse.success(null, "알림이 발송 큐에 등록되었습니다."))
    }
}
