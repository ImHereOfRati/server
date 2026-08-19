package com.kdongsu5509.notifications.application.port.out

import com.kdongsu5509.notifications.domain.MessageSendResult
import com.kdongsu5509.notifications.domain.SMS

interface ExternalMessagePort {
    fun send(sms: SMS): MessageSendResult
    fun sendMultiple(multiSMS: List<SMS>): List<MessageSendResult>
    fun findStatus(providerMessageId: String): MessageSendResult?
}
