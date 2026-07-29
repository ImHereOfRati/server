package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.adapter.out.firebase.RetryableFcmException
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.event.NotificationRequested
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service

@Service
class NotificationDeliveryService(
    private val recorder: NotificationRecorder,
    private val sender: NotificationSender,
    private val outcomeNotifier: NotificationOutcomeNotifier,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Retryable(
        retryFor = [RetryableFcmException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1_000, multiplier = 2.0, maxDelay = 8_000),
    )
    fun deliver(request: NotificationRequested) {
        val notification = try {
            recorder.reserve(request)
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
    fun recover(error: RetryableFcmException, request: NotificationRequested) {
        val notification = recorder.findByDedupeKey(
            Notification.dedupeKeyOf(request.eventId, request.notificationMethod)
        ) ?: return
        notifyFailure(notification, error)
    }

    @Recover
    fun recover(error: RetryableFcmException, notificationId: Long) {
        notifyFailure(recorder.findById(notificationId), error)
    }

    private fun attempt(notification: Notification) {
        val id = requireNotNull(notification.id) { "저장되지 않은 알림은 발송할 수 없습니다." }

        try {
            sender.send(notification)
            recorder.markSent(id)
            notifySuccess(notification)
        } catch (error: Exception) {
            val failed = recorder.markFailed(id, error)
            if (error !is RetryableFcmException) {
                notifyFailure(failed, error)
            }
            throw error
        }
    }

    private fun notifySuccess(notification: Notification) {
        runCatching { outcomeNotifier.success(notification) }
            .onFailure { log.error("알림 발송 성공 통지 중 오류", it) }
    }

    private fun notifyFailure(notification: Notification, error: Throwable) {
        runCatching { outcomeNotifier.failure(notification, error) }
            .onFailure { log.error("알림 발송 실패 통지 중 오류", it) }
    }
}
