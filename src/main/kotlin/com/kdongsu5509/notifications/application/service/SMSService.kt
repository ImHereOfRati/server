package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.port.`in`.MessageSendUseCase
import com.kdongsu5509.notifications.application.port.out.ExternalMessagePort
import com.kdongsu5509.notifications.domain.SMS
import org.springframework.stereotype.Service

@Service
class SMSService(private val externalMessagePort: ExternalMessagePort) : MessageSendUseCase {
    override fun send(senderNickname: String, receiverNumber: String, location: String) {
        //TODO : 문자 발송 데이터 저장 필요
        val sms = SMS(
            senderNickname = senderNickname,
            receiverNumber = receiverNumber,
            location = location
        )

        externalMessagePort.send(sms)
    }

    override fun sendMultiple(
        senderNickname: String,
        receiverNumbers: List<String>,
        location: String
    ) {
        val multipleSMS = receiverNumbers.map { SMS(senderNickname, it, location) }

        externalMessagePort.sendMultiple(multipleSMS)
    }
}
