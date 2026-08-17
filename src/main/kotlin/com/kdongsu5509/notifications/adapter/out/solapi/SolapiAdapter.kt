package com.kdongsu5509.notifications.adapter.out.solapi

import com.kdongsu5509.notifications.application.port.out.ExternalMessagePort
import com.kdongsu5509.notifications.domain.MessageSendResult
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
import java.lang.reflect.Method

@Component
class SolapiAdapter(
    private val externalSMSProperties: ExternalSMSProperties,
    private val solapiService: DefaultMessageService,
) : ExternalMessagePort {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun send(sms: SMS): MessageSendResult {
        return try {
            val response = solapiService.send(
                consistMessage(sms),
                null
            )
            log.info("단일 문자 발송 성공: {}", response)
            responseResult(response)
        } catch (e: Exception) {
            handleException("단일 발송", e)
        }
    }

    override fun sendMultiple(multiSMS: List<SMS>): List<MessageSendResult> {
        if (multiSMS.isEmpty()) {
            throw InvalidInputException("발송할 메시지가 없습니다.")
        }

        val messages = multiSMS.map { consistMessage(it) }
        return try {
            val result = solapiService.send(messages, null)
            log.info("다중 문자 발송 결과: {}", result)

            val detailList = result.messageList
            if (detailList.isEmpty()) {
                return List(multiSMS.size) { MessageSendResult.fail("응답 데이터가 비어있습니다.") }
            }

            detailList.map { detail ->
                if (detail.statusCode in listOf("200", "2000", "4000")) {
                    MessageSendResult.success()
                } else {
                    MessageSendResult.fail("[${detail.statusCode}] ${detail.statusMessage ?: "알 수 없는 에러"}")
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
        text = sms.body
    )

    /** Solapi may report provider failures in a normal response. */
    private fun responseResult(response: Any): MessageSendResult {
        val detail = responseObject(response, "getMessageList", "messageList", "getMessageList\$solapi_messaging")
            ?.let { (it as? Iterable<*>)?.firstOrNull() }
        val source = detail ?: response
        val statusCode = responseValue(source, "getStatusCode", "statusCode", "getStatusCode\$solapi_messaging")
        if (statusCode == null || statusCode in SUCCESS_STATUS_CODES) {
            return MessageSendResult.success()
        }

        val statusMessage = responseValue(source, "getStatusMessage", "statusMessage", "getStatusMessage\$solapi_messaging")
        return MessageSendResult.fail("[${statusCode}] ${statusMessage ?: "Solapi 메시지 발송 실패"}")
    }

    private fun responseObject(response: Any, vararg methodNames: String): Any? {
        for (methodName in methodNames) {
            val value = response.javaClass.methods
                .firstOrNull { it.parameterCount == 0 && it.name == methodName }
                ?.let { invokeOrNull(it, response) }
            if (value != null) return value
        }
        return null
    }

    private fun responseValue(response: Any, vararg methodNames: String): String? {
        for (methodName in methodNames) {
            val value = response.javaClass.methods
                .firstOrNull { it.parameterCount == 0 && it.name == methodName }
                ?.let { invokeOrNull(it, response)?.toString() }
            if (value != null) return value
        }
        return null
    }

    // Solapi 응답 타입이 버전에 따라 달라, 존재할 법한 게터를 차례로 호출해 본다.
    // 접근 불가/호출 실패는 다음 후보로 넘어가면 되므로 여기서 삼킨다.
    private fun invokeOrNull(method: Method, response: Any): Any? =
        try {
            method.invoke(response)
        } catch (error: Exception) {
            log.debug("Solapi 응답 게터 호출 실패 - method={}", method.name, error)
            null
        }

    private fun handleException(type: String, e: Exception): MessageSendResult {
        val errorMessage = when (e) {
            is SolapiBadRequestException -> "잘못된 요청: ${e.message}"
            is SolapiInvalidApiKeyException -> "잘못된 API 키: ${e.message}"
            is SolapiMessageNotReceivedException -> "발송 미접수: ${e.message}"
            else -> "시스템 오류: ${e.message ?: "Internal Error"}"
        }
        log.error("{} 실패 - {}", type, errorMessage, e)
        return MessageSendResult.fail(errorMessage)
    }

    private companion object {
        val SUCCESS_STATUS_CODES = setOf("200", "2000", "4000")
    }
}
