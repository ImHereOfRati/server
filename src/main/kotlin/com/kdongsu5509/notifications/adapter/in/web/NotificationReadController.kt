package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.notifications.adapter.`in`.web.dto.NotificationResponse
import com.kdongsu5509.notifications.application.port.`in`.NotificationUseCase
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/api/notifications", version = "1")
class NotificationReadController(
    private val notificationInboxUseCase: NotificationUseCase,
) {
    @GetMapping
    fun getNotifications(
        @AuthenticationPrincipal(expression = "userId") requesterID: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<NotificationResponse> =
        notificationInboxUseCase
            .findByRecipientId(requesterID, page, size)
            .map { NotificationResponse.from(it) }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PatchMapping("/{id}/read")
    fun markAsRead(
        @AuthenticationPrincipal(expression = "userId") requesterID: UUID,
        @PathVariable id: Long
    ) = notificationInboxUseCase.markAsRead(requesterID, id)
}
