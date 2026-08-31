package com.kdongsu5509.notifications.adapter.out.solapi

import com.kdongsu5509.notifications.application.port.out.ExternalMessagePort
import com.kdongsu5509.notifications.domain.MessageSendResult
import com.kdongsu5509.notifications.domain.SMS
import com.kdongsu5509.support.config.ExternalSMSProperties
import com.kdongsu5509.support.exception.type.InvalidInputException
import com.solapi.sdk.message.dto.request.MessageListRequest
import com.solapi.sdk.message.exception.SolapiBadRequestException
import com.solapi.sdk.message.exception.SolapiInvalidApiKeyException
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException
import com.solapi.sdk.message.model.Message
import com.solapi.sdk.message.service.DefaultMessageService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Profile
import java.lang.reflect.Method

@Component
@Profile("!loadtest")
class SolapiAdapter(
    private val externalSMSProperties: ExternalSMSProperties,
    private val solapiService: DefaultMessageService,
) : ExternalMessagePort {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun send(sms: SMS): MessageSendResult = try {
        val response = solapiService.send(consistMessage(sms), null)
        log.info("Solapi SMS send response: {}", response)
        responseResult(response)
    } catch (e: Exception) {
        handleException("single SMS send", e)
    }

    override fun findStatus(providerMessageId: String): MessageSendResult? = try {
        val response = solapiService.getMessageList(
            MessageListRequest().apply { messageIds = listOf(providerMessageId) }
        )
        val messages = response?.messageList.orEmpty()
        val detail = messages[providerMessageId]
            ?: messages.values.firstOrNull()
            ?: return null
        resultFromDetail(detail)
    } catch (e: Exception) {
        log.warn("Solapi message status lookup failed - messageId={}", providerMessageId, e)
        MessageSendResult.unknown("Solapi status lookup failed: ${e.message}", providerMessageId)
    }

    override fun sendMultiple(multiSMS: List<SMS>): List<MessageSendResult> {
        if (multiSMS.isEmpty()) throw InvalidInputException("messages must not be empty")
        return try {
            val result = solapiService.send(multiSMS.map(::consistMessage), null)
            if (result.messageList.isEmpty()) {
                return List(multiSMS.size) { MessageSendResult.fail("응답 데이터가 비어있습니다.") }
            }
            result.messageList.map { detail ->
                resultFromStatus(detail.statusCode, detail.statusMessage, detail.messageId)
            }
        } catch (e: Exception) {
            val failure = handleException("multiple SMS send", e)
            List(multiSMS.size) { failure }
        }
    }

    private fun consistMessage(sms: SMS): Message = Message(
        from = externalSMSProperties.sender,
        to = sms.receiverNumber,
        text = sms.body,
    ).apply {
        customFields = mutableMapOf("imhereReceiver" to sms.receiverNumber)
    }

    private fun responseResult(response: Any): MessageSendResult {
        val detail = responseObject(response, "getMessageList", "messageList", "getMessageList\$solapi_messaging")
            ?.let { (it as? Iterable<*>)?.firstOrNull() }
        val source = detail ?: response
        return resultFromStatus(
            responseValue(source, "getStatusCode", "statusCode", "getStatusCode\$solapi_messaging"),
            responseValue(source, "getStatusMessage", "statusMessage", "getStatusMessage\$solapi_messaging"),
            responseValue(source, "getMessageId", "messageId", "getMessageId\$solapi_messaging"),
        )
    }

    private fun resultFromDetail(detail: Message): MessageSendResult =
        resultFromStatus(detail.statusCode, detail.status, detail.messageId)

    private fun resultFromStatus(statusCode: String?, statusMessage: String?, messageId: String?): MessageSendResult =
        when {
            statusCode == null || statusCode in SUCCESS_STATUS_CODES -> MessageSendResult.success(messageId)
            statusCode == "2000" || statusCode == "3000" -> MessageSendResult.unknown(
                "[$statusCode] ${statusMessage ?: "Solapi processing"}", messageId
            )
            else -> MessageSendResult.fail("[$statusCode] ${statusMessage ?: "Solapi send failed"}", messageId)
        }

    private fun responseObject(response: Any, vararg methodNames: String): Any? = methodNames.firstNotNullOfOrNull { name ->
        response.javaClass.methods.firstOrNull { it.parameterCount == 0 && it.name == name }?.let { invokeOrNull(it, response) }
    }

    private fun responseValue(response: Any, vararg methodNames: String): String? = methodNames.firstNotNullOfOrNull { name ->
        response.javaClass.methods.firstOrNull { it.parameterCount == 0 && it.name == name }
            ?.let { invokeOrNull(it, response)?.toString() }
    }

    private fun invokeOrNull(method: Method, response: Any): Any? = try {
        method.invoke(response)
    } catch (error: Exception) {
        log.debug("Solapi response getter failed - method={}", method.name, error)
        null
    }

    private fun handleException(type: String, e: Exception): MessageSendResult {
        val errorMessage = when (e) {
            is SolapiBadRequestException -> "잘못된 요청: ${e.message}"
            is SolapiInvalidApiKeyException -> "잘못된 API 키: ${e.message}"
            is SolapiMessageNotReceivedException -> "발송 미접수: ${e.message}"
            else -> "provider error: ${e.message ?: "Internal Error"}"
        }
        log.error("{} failed - {}", type, errorMessage, e)
        return if (e is SolapiBadRequestException || e is SolapiInvalidApiKeyException || e is SolapiMessageNotReceivedException) {
            MessageSendResult.fail(errorMessage)
        } else {
            MessageSendResult.unknown(errorMessage)
        }
    }

    private companion object {
        val SUCCESS_STATUS_CODES = setOf("200", "2000", "4000")
    }
}
