package com.kdongsu5509.notifications.adapter.out.solapi

import com.kdongsu5509.notifications.application.port.out.ExternalMessagePort
import com.kdongsu5509.notifications.domain.SMS
import com.kdongsu5509.support.config.ExternalSMSProperties
import com.kdongsu5509.support.exception.type.InvalidInputException
import com.solapi.sdk.message.exception.SolapiBadRequestException
import com.solapi.sdk.message.exception.SolapiInvalidApiKeyException
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException
import com.solapi.sdk.message.model.Message
import com.solapi.sdk.message.service.DefaultMessageService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SolapiAdapter(
    private val externalSMSProperties: ExternalSMSProperties,
    private val solapiService: DefaultMessageService,
) : ExternalMessagePort {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun send(sms: SMS): SolapiResponse {
        return try {
            val response = solapiService.send(
                consistMessage(sms),
                null
            )
            log.info("단일 문자 발송 성공: {}", response)
            SolapiResponse.success()
        } catch (e: Exception) {
            handleException("단일 발송", e)
        }
    }

    override fun sendMultiple(multiSMS: List<SMS>): List<SolapiResponse> {
        if (multiSMS.isEmpty()) {
            throw InvalidInputException("발송할 메시지가 없습니다.")
        }

        val messages = multiSMS.map { consistMessage(it) }
        return try {
            val result = solapiService.send(messages, null)
            log.info("다중 문자 발송 결과: {}", result)

            val detailList = result.messageList
            if (detailList.isEmpty()) {
                return List(multiSMS.size) { SolapiResponse.fail("응답 데이터가 비어있습니다.") }
            }

            detailList.map { detail ->
                if (detail.statusCode in listOf("200", "2000", "4000")) {
                    SolapiResponse.success()
                } else {
                    SolapiResponse.fail("[${detail.statusCode}] ${detail.statusMessage ?: "알 수 없는 에러"}")
                }
            }
        } catch (e: Exception) {
            val failResponse = handleException("다중 발송", e)
            List(multiSMS.size) { failResponse }
        }
    }

    private fun consistMessage(sms: SMS): Message = Message(
        from = externalSMSProperties.sender,
        to = sms.receiverNumber,
        text = sms.buildMessageText()
    )

    private fun handleException(type: String, e: Exception): SolapiResponse {
        val errorMessage = when (e) {
            is SolapiBadRequestException -> "잘못된 요청: ${e.message}"
            is SolapiInvalidApiKeyException -> "잘못된 API 키: ${e.message}"
            is SolapiMessageNotReceivedException -> "발송 미접수: ${e.message}"
            else -> "시스템 오류: ${e.message ?: "Internal Error"}"
        }
        log.error("{} 실패 - {}", type, errorMessage, e)
        return SolapiResponse.fail(errorMessage)
    }
}
