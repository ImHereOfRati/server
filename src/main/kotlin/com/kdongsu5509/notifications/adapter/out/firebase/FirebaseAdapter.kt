package com.kdongsu5509.notifications.adapter.out.firebase

import com.google.firebase.messaging.*
import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationType
import com.kdongsu5509.notifications.application.port.out.FirebasePort
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.support.exception.type.InternalServerException
import com.kdongsu5509.support.exception.type.InvalidInputException
import com.kdongsu5509.support.exception.type.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FirebaseAdapter(private val firebaseMessaging: FirebaseMessaging) : FirebasePort {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun send(fcmToken: String, title: String, body: String, data: Map<String, String>) {
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
        throw InternalServerException("알 수 없는 FCM 오류가 발생했습니다.", cause = ex)
    }

    private fun handleUnregistered(ex: FirebaseMessagingException) =
        if (ex.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
            log.error("등록 해제된 토큰. DB 삭제 필요")
            throw NotFoundException("등록 해제된 FCM 토큰입니다.", contextData = mapOf("unregistered" to true))
        } else Unit

    private fun handleRetryable(ex: FirebaseMessagingException) {
        val code = ex.messagingErrorCode
        val retryableSet =
            setOf(MessagingErrorCode.UNAVAILABLE, MessagingErrorCode.QUOTA_EXCEEDED, MessagingErrorCode.INTERNAL)
        if (code in retryableSet) throw logAndReturnRetryable(code, ex)
    }

    private fun handleNonRetryable(ex: FirebaseMessagingException) = when (ex.messagingErrorCode) {
        MessagingErrorCode.INVALID_ARGUMENT -> logAndThrow(InvalidInputException("FCM 요청 매개변수가 잘못되었습니다."), ex)
        MessagingErrorCode.SENDER_ID_MISMATCH -> logAndThrow(InternalServerException("FCM 발신자 ID가 일치하지 않습니다."), ex)
        MessagingErrorCode.THIRD_PARTY_AUTH_ERROR -> logAndThrow(
            InternalServerException("FCM 타사 인증 오류가 발생했습니다."),
            ex
        )

        else -> Unit
    }

    private fun createFcmMessage(token: String, title: String, body: String, data: Map<String, String>) =
        Message.builder()
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .putAllData(data)
            .setAndroidConfig(createAndroidConfig(data))
            .setToken(token).build()

    private fun createAndroidConfig(data: Map<String, String>): AndroidConfig {
        val notificationType = NotificationType.fromName(data["type"]) ?: NotificationType.DELIVERY_RESULT_NOTICE

        return AndroidConfig.builder()
            .setPriority(notificationType.androidPriority)
            .setNotification(
                AndroidNotification.builder()
                    .setChannelId(notificationType.androidChannelId)
                    .build()
            )
            .build()
    }

    private fun logAndThrow(exception: ImHereBaseException, ex: Exception): Nothing {
        log.error("[${exception.errorCode}] ${exception.message}", ex)
        throw exception
    }

    private fun logAndReturnRetryable(code: MessagingErrorCode, ex: Exception): RetryableFcmException {
        log.error("[$code] FCM 서버 일시적 오류. 재시도 시작.", ex)
        return RetryableFcmException("FCM 재시도 필요: $code", ex)
    }
}

class RetryableFcmException(message: String, cause: Throwable) : RuntimeException(message, cause)
