package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.notifications.adapter.`in`.web.dto.NotificationInboxResponse
import com.kdongsu5509.notifications.application.port.`in`.NotificationInboxUseCase
import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.toOkResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications", version = "1")
class NotificationReadController(
    private val notificationInboxUseCase: NotificationInboxUseCase,
) {
    @GetMapping
    fun getNotifications(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<List<NotificationInboxResponse>>> =
        notificationInboxUseCase
            .findByReceiverEmail(userDetails.email, page, size)
            .map { NotificationInboxResponse.from(it) }
            .toOkResponse()

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{id}/read")
    fun markAsRead(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @PathVariable id: Long
    ) = notificationInboxUseCase.markAsRead(userDetails.email, id)
}
