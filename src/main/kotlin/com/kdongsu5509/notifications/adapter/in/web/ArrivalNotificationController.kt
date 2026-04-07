package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationType
import com.kdongsu5509.notifications.adapter.`in`.web.dto.FcmNotificationRequest
import com.kdongsu5509.notifications.application.port.`in`.NotificationToUserCasePort
import com.kdongsu5509.user.application.service.user.SimpleTokenUserDetails
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for Arrival confirmation notifications (NOTI-006).
 */
@RestController
@RequestMapping("/api/notification/fcm/arrival", version = "1")
class ArrivalNotificationController(
    private val notificationToUserCasePort: NotificationToUserCasePort
) {

    @PostMapping
    fun send(
        @AuthenticationPrincipal user: SimpleTokenUserDetails,
        @Validated @RequestBody request: FcmNotificationRequest
    ) {
        notificationToUserCasePort.send(
            senderNickname = user.nickname,
            senderEmail = user.username,
            receiverEmail = request.receiverEmail,
            type = NotificationType.ARRIVAL_CONFIRMATION.name,
            body = request.body
        )
    }
}
