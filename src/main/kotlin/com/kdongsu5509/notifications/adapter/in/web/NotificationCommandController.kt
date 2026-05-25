package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.notifications.adapter.`in`.web.dto.MultiNotificationRequest
import com.kdongsu5509.notifications.adapter.`in`.web.dto.NotificationRequest
import com.kdongsu5509.notifications.application.port.`in`.NotificationEnqueueUseCase
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications", version = "1")
class NotificationCommandController(
    private val notificationEnqueueUseCase: NotificationEnqueueUseCase
) {
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun send(
        @AuthenticationPrincipal user: ImHereUserDetails,
        @Validated @RequestBody request: NotificationRequest
    ) = notificationEnqueueUseCase.enqueue(
        request.toCommand(user.nickname, user.username)
    )

    @PostMapping("/batch")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun sendMultiple(
        @AuthenticationPrincipal user: ImHereUserDetails,
        @Validated @RequestBody request: MultiNotificationRequest
    ) = notificationEnqueueUseCase.enqueueMultiple(
        request.toCommand(user.nickname, user.username)
    )
}
