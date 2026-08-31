package com.kdongsu5509.notifications.adapter.out.firebase

import com.google.firebase.messaging.*
import com.kdongsu5509.notifications.application.port.out.FirebasePort
import com.kdongsu5509.notifications.domain.AndroidPushPriority
import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.PushChannel
import com.kdongsu5509.notifications.domain.RenderedNotification
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.notifications.exception.RetryableFcmException
import com.kdongsu5509.notifications.exception.UnregisteredTokenException
import com.kdongsu5509.support.exception.throwIt
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.context.annotation.Profile

@Component
@Profile("!loadtest")
class FirebaseAdapter(private val firebaseMessaging: FirebaseMessaging) : FirebasePort {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun send(fcmToken: String, deviceType: DeviceType, rendered: RenderedNotification) {
        if (fcmToken.isBlank()) return log.warn("FCM 토큰 공백으로 전송 중단")
        try {
            firebaseMessaging.send(createFcmMessage(fcmToken, deviceType, rendered))
        } catch (ex: FirebaseMessagingException) {
            processFcmException(ex)
        }
    }

    private fun processFcmException(ex: FirebaseMessagingException): Nothing {
        handleUnregistered(ex)
        handleRetryable(ex)
        handleNonRetryable(ex)
        fail(NotificationException.FCM_UNKNOWN_ERROR, ex)
    }

    private fun handleUnregistered(ex: FirebaseMessagingException) =
        if (ex.messagingErrorCode == MessagingErrorCode.UNREGISTERED) {
            log.error("등록 해제된 토큰. DB 삭제 필요")
            throw UnregisteredTokenException(cause = ex)
        } else Unit

    private fun handleRetryable(ex: FirebaseMessagingException) {
        val code = ex.messagingErrorCode
        val retryableSet =
            setOf(MessagingErrorCode.UNAVAILABLE, MessagingErrorCode.QUOTA_EXCEEDED, MessagingErrorCode.INTERNAL)
        if (code in retryableSet) throw logAndReturnRetryable(code, ex)
    }

    private fun handleNonRetryable(ex: FirebaseMessagingException) = when (ex.messagingErrorCode) {
        MessagingErrorCode.INVALID_ARGUMENT ->
            fail(NotificationException.FCM_INVALID_ARGUMENT, ex, "FCM 요청 매개변수가 잘못되었습니다.")

        MessagingErrorCode.SENDER_ID_MISMATCH ->
            fail(NotificationException.FCM_AUTH_ERROR, ex, "FCM 발신자 ID가 일치하지 않습니다.")

        MessagingErrorCode.THIRD_PARTY_AUTH_ERROR ->
            fail(NotificationException.FCM_AUTH_ERROR, ex, "FCM 타사 인증 오류가 발생했습니다.")

        else -> Unit
    }

    private fun createFcmMessage(
        token: String,
        deviceType: DeviceType,
        rendered: RenderedNotification,
    ): Message {
        val builder = Message.builder()
            .setNotification(
                Notification.builder()
                    .setTitle(rendered.title)
                    .setBody(rendered.body)
                    .build()
            )
            .putAllData(rendered.data)
            .setToken(token)

        val channel = rendered.channel
        return when (deviceType) {
            DeviceType.AOS -> builder.setAndroidConfig(createAndroidConfig(channel))
            DeviceType.IOS -> builder.setApnsConfig(createApnsConfig(channel))
        }.build()
    }

    private fun createAndroidConfig(channel: PushChannel): AndroidConfig =
        AndroidConfig.builder()
            .setPriority(toFirebasePriority(channel.androidPriority))
            .setNotification(
                AndroidNotification.builder()
                    .setChannelId(channel.androidChannelId)
                    .build()
            )
            .build()

    private fun createApnsConfig(channel: PushChannel): ApnsConfig {
        val aps = Aps.builder()
            .putCustomData(INTERRUPTION_LEVEL_KEY, channel.interruptionLevel.value)
            .also { builder -> channel.sound?.let { builder.setSound(it) } }
            .build()

        return ApnsConfig.builder()
            .putHeader(APNS_PRIORITY_HEADER, channel.apnsPriority.headerValue)
            .setAps(aps)
            .build()
    }

    private fun toFirebasePriority(priority: AndroidPushPriority): AndroidConfig.Priority =
        when (priority) {
            AndroidPushPriority.HIGH -> AndroidConfig.Priority.HIGH
            AndroidPushPriority.NORMAL -> AndroidConfig.Priority.NORMAL
        }

    private fun fail(
        errorCode: NotificationException,
        ex: FirebaseMessagingException,
        message: String? = null,
    ): Nothing {
        log.error("[{}] {}", errorCode.imhereErrorCode, message ?: errorCode.errorMessage, ex)
        errorCode.throwIt(
            contextData = mapOf("messagingErrorCode" to ex.messagingErrorCode?.name),
            customMessage = message,
            cause = ex,
        )
    }

    private fun logAndReturnRetryable(code: MessagingErrorCode, ex: Exception): RetryableFcmException {
        log.error("[$code] FCM 서버 일시적 오류. 재시도 시작.", ex)
        return RetryableFcmException("FCM 재시도 필요: $code", ex)
    }

    private companion object {
        const val APNS_PRIORITY_HEADER = "apns-priority"
        const val INTERRUPTION_LEVEL_KEY = "interruption-level"
    }
}
