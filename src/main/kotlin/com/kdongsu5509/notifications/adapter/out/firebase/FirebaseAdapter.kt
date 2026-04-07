package com.kdongsu5509.notifications.adapter.out.firebase

import com.google.firebase.messaging.*
import com.kdongsu5509.notifications.application.port.out.FirebasePort
import com.kdongsu5509.notifications.domain.FCMMessageTitle
import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.FCMErrorCode
import org.slf4j.LoggerFactory
import org.springframework.resilience.annotation.Retryable
import org.springframework.stereotype.Component

@Component
class FirebaseAdapter(private val firebaseMessaging: FirebaseMessaging) : FirebasePort {
    private val log = LoggerFactory.getLogger(this::class.java)

    @Retryable(value = [RetryableFcmException::class], delay = 1000, multiplier = 2.0)
    override fun send(fcmToken: String, title: FCMMessageTitle, body: String, data: Map<String, String>) {
        if (fcmToken.isBlank()) return log.warn("FCM 토큰 공백으로 전송 중단")
        try {
            firebaseMessaging.send(createFcmMessage(fcmToken, title, body, data))
        } catch (ex: FirebaseMessagingException) {
            processFcmException(ex)
        }
    }

    private fun processFcmException(ex: FirebaseMessagingException) {
        handleUnregistered(ex)
        handleRetryable(ex)
        handleNonRetryable(ex)
        throw BusinessException(FCMErrorCode.FCM_UNKNOWN_ERROR)
    }

    private fun handleUnregistered(ex: FirebaseMessagingException) =
        if (ex.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
            log.error("등록 해제된 토큰. DB 삭제 필요")
            throw BusinessException(FCMErrorCode.FCM_TOKEN_UNREGISTERED)
        } else Unit

    private fun handleRetryable(ex: FirebaseMessagingException) {
        val code = ex.messagingErrorCode
        val retryableSet =
            setOf(MessagingErrorCode.UNAVAILABLE, MessagingErrorCode.QUOTA_EXCEEDED, MessagingErrorCode.INTERNAL)
        if (code in retryableSet) throw logAndReturnRetryable(code, ex)
    }

    private fun handleNonRetryable(ex: FirebaseMessagingException) = when (ex.messagingErrorCode) {
        MessagingErrorCode.INVALID_ARGUMENT -> logAndThrow(FCMErrorCode.FCM_INVALID_ARGUMENT, "잘못된 매개변수", ex)
        MessagingErrorCode.SENDER_ID_MISMATCH -> logAndThrow(FCMErrorCode.FCM_AUTH_ERROR, "발신자 ID 불일치", ex)
        MessagingErrorCode.THIRD_PARTY_AUTH_ERROR -> logAndThrow(FCMErrorCode.FCM_AUTH_ERROR, "타사 인증 오류", ex)
        else -> Unit
    }

    private fun createFcmMessage(token: String, title: FCMMessageTitle, body: String, data: Map<String, String>) =
        Message.builder()
            .setNotification(Notification.builder().setTitle(title.content).setBody(body).build())
            .putAllData(data)
            .setToken(token).build()

    private fun logAndThrow(err: FCMErrorCode, msg: String, ex: Exception): Nothing {
        log.error("[$err] $msg", ex)
        throw BusinessException(err)
    }

    private fun logAndReturnRetryable(code: MessagingErrorCode, ex: Exception): RetryableFcmException {
        log.error("[$code] FCM 서버 일시적 오류. 재시도 시작.", ex)
        return RetryableFcmException("FCM 재시도 필요: $code", ex)
    }
}

class RetryableFcmException(message: String, cause: Throwable) : RuntimeException(message, cause)