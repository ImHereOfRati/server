package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.notifications.adapter.`in`.web.dto.NotificationHistoryResponse
import com.kdongsu5509.notifications.application.port.`in`.NotificationHistoryUseCase
import com.kdongsu5509.shared.response.ApiResponse
import com.kdongsu5509.shared.response.toOkResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications", version = "1")
class NotificationReadController(
    private val notificationHistoryUseCase: NotificationHistoryUseCase,
) {
    @GetMapping
    fun getNotifications(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<List<NotificationHistoryResponse>>> =
        notificationHistoryUseCase
            .findByReceiverEmail(userDetails.email, page, size)
            .map { NotificationHistoryResponse.from(it) }
            .toOkResponse()

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{id}/read")
    fun markAsRead(
        @AuthenticationPrincipal userDetails: ImHereUserDetails,
        @PathVariable id: Long
    ) = notificationHistoryUseCase.markAsRead(userDetails.email, id)
}
