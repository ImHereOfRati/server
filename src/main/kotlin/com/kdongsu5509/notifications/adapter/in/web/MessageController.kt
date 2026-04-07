package com.kdongsu5509.notifications.adapter.`in`.web

import com.kdongsu5509.notifications.adapter.`in`.web.dto.MessageSendRequest
import com.kdongsu5509.notifications.adapter.`in`.web.dto.MultiMessageSendRequest
import com.kdongsu5509.notifications.application.port.`in`.MessageSendUseCasePort
import com.kdongsu5509.user.application.service.user.SimpleTokenUserDetails
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notification/sms/send", version = "1")
class MessageController(
    private val messageSendUseCasePort: MessageSendUseCasePort,
) {

    @PostMapping
    fun send(
        @AuthenticationPrincipal user: SimpleTokenUserDetails,
        @RequestBody request: MessageSendRequest
    ) {
        val nickname = user.nickname
        messageSendUseCasePort.send(nickname, request.receiverNumber, request.location)
    }

    @PostMapping("/multi")
    fun sendMultiple(
        @AuthenticationPrincipal user: SimpleTokenUserDetails,
        @RequestBody request: MultiMessageSendRequest
    ) {
        val nickname = user.nickname
        messageSendUseCasePort.sendMultiple(nickname, request.receiversNumbers, request.location)
    }
}
