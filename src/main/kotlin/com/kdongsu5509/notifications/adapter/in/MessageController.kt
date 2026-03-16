package com.kdongsu5509.notifications.adapter.`in`

import com.kdongsu5509.imhere.message.adapter.dto.MessageSendRequest
import com.kdongsu5509.imhere.message.adapter.dto.MultipleMessageSendRequest
import lombok.extern.slf4j.Slf4j
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Slf4j
@RestController
@RequestMapping("/api/v1/message")
class MessageController(
    private val singleMessageSendUseCasePort: SingleMessageSendUseCasePort,
    private val multipleMessageSendUseCasePort: MultipleMessageSendUseCasePort
) {

    @PostMapping("/send")
    fun send(@RequestBody request: MessageSendRequest, @AuthenticationPrincipal user: UserDetails) {
        val email = user.username
        singleMessageSendUseCasePort.send(request, email)
    }

    @PostMapping("/multipleSend")
    fun sendMultiple(@RequestBody request: MultipleMessageSendRequest, @AuthenticationPrincipal user: UserDetails) {
        val email = user.username
        multipleMessageSendUseCasePort.send(request, email)
    }
}