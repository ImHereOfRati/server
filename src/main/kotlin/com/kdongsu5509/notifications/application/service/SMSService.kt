package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.port.`in`.MessageSendUseCase
import com.kdongsu5509.notifications.application.port.out.ExternalMessagePort
import com.kdongsu5509.notifications.adapter.out.solapi.SolapiResponse
import com.kdongsu5509.notifications.domain.SMS
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt
import org.springframework.stereotype.Service

@Service
class SMSService(private val externalMessagePort: ExternalMessagePort) : MessageSendUseCase {
    override fun send(senderNickname: String, receiverNumber: String, location: String) {
        //TODO : 문자 발송 데이터 저장 필요
        validateReceiverNumber(receiverNumber)
        val sms = SMS(
            senderNickname = senderNickname,
            receiverNumber = receiverNumber,
            location = location
        )

        val response = externalMessagePort.send(sms)
        ensureSendSucceeded(response, receiverNumber)
    }

    override fun sendMultiple(
        senderNickname: String,
        receiverNumbers: List<String>,
        location: String
    ) {
        validateReceiverNumbers(receiverNumbers)
        val multipleSMS = receiverNumbers.map { SMS(senderNickname, it, location) }

        val responses = externalMessagePort.sendMultiple(multipleSMS)
        if (responses.size != multipleSMS.size || responses.any { it.status != SolapiResponse.SUCCESS_STATUS }) {
            NotificationException.SMS_SEND_FAILED.throwIt(
                contextData = mapOf(
                    "receiverCount" to receiverNumbers.size,
                    "responseCount" to responses.size,
                    "failedCount" to responses.count { it.status != SolapiResponse.SUCCESS_STATUS }
                )
            )
        }
    }

    private fun validateReceiverNumber(receiverNumber: String) {
        if (receiverNumber.isBlank()) {
            NotificationException.UNSUPPORTED_TARGET_TYPE.throwIt(
                contextData = mapOf("receiverNumber" to receiverNumber)
            )
        }
    }

    private fun validateReceiverNumbers(receiverNumbers: List<String>) {
        if (receiverNumbers.isEmpty() || receiverNumbers.any { it.isBlank() }) {
            NotificationException.UNSUPPORTED_TARGET_TYPE.throwIt(
                contextData = mapOf("receiverCount" to receiverNumbers.size)
            )
        }
    }

    private fun ensureSendSucceeded(response: SolapiResponse, receiverNumber: String) {
        if (response.status != SolapiResponse.SUCCESS_STATUS) {
            NotificationException.SMS_SEND_FAILED.throwIt(
                contextData = mapOf(
                    "receiverNumber" to receiverNumber,
                    "status" to response.status,
                    "message" to response.message
                )
            )
        }
    }
}
