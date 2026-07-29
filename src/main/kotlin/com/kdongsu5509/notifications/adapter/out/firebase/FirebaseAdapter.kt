package com.kdongsu5509.notifications.adapter.out.firebase

import com.google.firebase.messaging.*
import com.kdongsu5509.notifications.application.port.out.FirebasePort
import com.kdongsu5509.notifications.domain.AndroidPushPriority
import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.PushChannel
import com.kdongsu5509.notifications.domain.RenderedNotification
import com.kdongsu5509.notifications.exception.UnregisteredTokenException
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.support.exception.type.InternalServerException
import com.kdongsu5509.support.exception.type.InvalidInputException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
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

    private fun processFcmException(ex: FirebaseMessagingException) {
        handleUnregistered(ex)
        handleRetryable(ex)
        handleNonRetryable(ex)
        throw InternalServerException("알 수 없는 FCM 오류가 발생했습니다.", cause = ex)
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
        MessagingErrorCode.INVALID_ARGUMENT -> logAndThrow(InvalidInputException("FCM 요청 매개변수가 잘못되었습니다."), ex)
        MessagingErrorCode.SENDER_ID_MISMATCH -> logAndThrow(InternalServerException("FCM 발신자 ID가 일치하지 않습니다."), ex)
        MessagingErrorCode.THIRD_PARTY_AUTH_ERROR -> logAndThrow(
            InternalServerException("FCM 타사 인증 오류가 발생했습니다."),
            ex
        )

        else -> Unit
    }

    /**
     * 기기 종류에 따라 Android/iOS 중 한쪽 전달 설정만 붙인다.
     *
     * 어떤 정책을 쓸지는 [PushChannel]이 이미 정해 두었으므로 여기서는 SDK 타입으로 옮기기만 한다.
     */
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

        val channel = rendered.type.channel
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

    private fun logAndThrow(exception: ImHereBaseException, ex: Exception): Nothing {
        log.error("[${exception.errorCode}] ${exception.message}", ex)
        throw exception
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

class RetryableFcmException(message: String, cause: Throwable) : RuntimeException(message, cause)
