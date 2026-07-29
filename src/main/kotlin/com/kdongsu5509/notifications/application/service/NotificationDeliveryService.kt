package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.adapter.out.firebase.RetryableFcmException
import com.kdongsu5509.notifications.application.dto.NotificationDeliveryCommand
import com.kdongsu5509.notifications.application.service.channel.NotificationDeliveryChannel
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service

@Service
class NotificationDeliveryService(
    channels: List<NotificationDeliveryChannel>,
    private val recorder: NotificationRecorder,
    private val failureNotifier: NotificationFailureNotifier,
    private val receiptPublisher: NotificationReceiptPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val channels = channels.associateBy(NotificationDeliveryChannel::method)

    @Retryable(
        retryFor = [RetryableFcmException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1_000, multiplier = 2.0, maxDelay = 8_000),
    )
    fun deliver(command: NotificationDeliveryCommand) {
        val notification = try {
            recorder.reserve(command)
        } catch (_: DataIntegrityViolationException) {
            return
        } ?: return

        attempt(notification)
    }

    @Retryable(
        retryFor = [RetryableFcmException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1_000, multiplier = 2.0, maxDelay = 8_000),
    )
    fun redeliver(notificationId: Long) {
        val notification = recorder.findById(notificationId)
        if (!notification.isDeliverable) return
        attempt(notification)
    }

    @Recover
    fun recover(error: RetryableFcmException, command: NotificationDeliveryCommand) {
        val notification = recorder.findByDedupeKey(
            Notification.dedupeKeyOf(command.eventId, command.notificationMethod)
        ) ?: return
        failureNotifier.notifyFailure(notification, error)
    }

    @Recover
    fun recover(error: RetryableFcmException, notificationId: Long) {
        failureNotifier.notifyFailure(recorder.findById(notificationId), error)
    }

    private fun attempt(notification: Notification) {
        val id = requireNotNull(notification.id) { "저장되지 않은 알림은 발송할 수 없습니다." }
        val channel = channels[notification.method]
            ?: NotificationException.UNSUPPORTED_TARGET_TYPE.throwIt()

        try {
            channel.send(notification)
            recorder.markSent(id)
            publishSuccessReceipt(notification)
        } catch (error: Exception) {
            val failed = recorder.markFailed(id, error)
            if (error !is RetryableFcmException) {
                failureNotifier.notifyFailure(failed, error)
            }
            throw error
        }
    }

    private fun publishSuccessReceipt(notification: Notification) {
        if (notification.type.isMeta) return
        runCatching {
            receiptPublisher.publish(notification, NotificationType.DELIVERY_RESULT_NOTICE)
        }.onFailure { log.error("발송 결과 알림 이벤트 발행 중 오류", it) }
    }
}
