package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationStatus
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.Notification.Companion.MAX_ATTEMPTS
import com.kdongsu5509.notifications.event.NotificationEvent
import com.kdongsu5509.notifications.exception.RetryableFcmException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import java.util.*

@Service
class NotificationDeliveryFacade(
    private val notificationRegister: NotificationRegister,
    private val notificationChannelSender: NotificationChannelSender,
    private val deliveryResultNotifier: DeliveryResultNotifier,
) {
    @Retryable(
        retryFor = [RetryableFcmException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1_000, multiplier = 2.0, maxDelay = 8_000),
    )
    fun deliver(event: NotificationEvent) {
        val notification = resolveDeliveryTarget(event) ?: return
        val claimed = notificationRegister.claimForDelivery(requireNotNull(notification.id)) ?: return
        sendAndRecordOutcome(claimed, requesterId = event.senderId)
    }

    @Retryable(
        retryFor = [RetryableFcmException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1_000, multiplier = 2.0, maxDelay = 8_000),
    )
    fun redeliver(notificationId: Long) {
        val notification = notificationRegister.getByIdOrThrow(notificationId)
        val claimed = notificationRegister.claimForDelivery(notificationId) ?: return
        sendAndRecordOutcome(claimed, requesterId = null)
    }

    @Recover
    fun notifyFailureAfterRetriesExhausted(error: RetryableFcmException, event: NotificationEvent) {
        val notification = notificationRegister.findByDedupeKey(
            Notification.dedupeKeyOf(event.eventId, event.notificationMethod)
        ) ?: return
        deliveryResultNotifier.notifyDeliveryFailed(notification, event.senderId, error)
    }

    @Recover
    fun notifyFailureAfterRetriesExhausted(error: RetryableFcmException, notificationId: Long) {
        deliveryResultNotifier.notifyDeliveryFailed(
            notification = notificationRegister.getByIdOrThrow(notificationId),
            requesterId = null,
            error = error,
        )
    }

    private fun resolveDeliveryTarget(event: NotificationEvent): Notification? {
        val dedupeKey = Notification.dedupeKeyOf(event.eventId, event.notificationMethod)
        val alreadyRegistered = notificationRegister.findByDedupeKey(dedupeKey)

        if (alreadyRegistered != null) {
            return alreadyRegistered.takeIf { it.status == NotificationStatus.FAILED }
        }

        return try {
            notificationRegister.register(event)
        } catch (_: DataIntegrityViolationException) {
            null
        }
    }

    @Recover
    fun rethrowNonRetryableError(error: Throwable, event: NotificationEvent): Unit = throw error

    @Recover
    fun rethrowNonRetryableError(error: Throwable, notificationId: Long): Unit = throw error

    private fun sendAndRecordOutcome(notification: Notification, requesterId: UUID?) {
        val id = requireNotNull(notification.id) { "저장되지 않은 알림은 발송할 수 없습니다." }

        try {
            if (notification.method == NotificationMethod.SMS && notification.attempts >= MAX_ATTEMPTS - 1) {
                val dead = notificationRegister.markDead(id, "SMS retry limit reached; provider call skipped")
                deliveryResultNotifier.notifyDeliveryFailed(
                    dead,
                    requesterId,
                    IllegalStateException("SMS 최종 실패: 재발송하지 않고 Discord로 알립니다."),
                )
                return
            }
            val result = notificationChannelSender.sendViaExternalMethod(notification)
            when {
                result == null || result.isSuccess -> {
                    notificationRegister.markAsSent(id, result)
                    deliveryResultNotifier.notifyDeliverySucceeded(notification, requesterId)
                }
                result.certainty == com.kdongsu5509.notifications.domain.DeliveryCertainty.UNKNOWN -> {
                    notificationRegister.markUnknown(id, result)
                }
                else -> {
                    val failed = notificationRegister.markFailed(id, result.message)
                    deliveryResultNotifier.notifyDeliveryFailed(failed, requesterId, RuntimeException(result.message))
                }
            }
        } catch (error: Exception) {
            val failed = notificationRegister.markFailed(id, error.message ?: error.javaClass.simpleName)
            if (error !is RetryableFcmException) {
                deliveryResultNotifier.notifyDeliveryFailed(failed, requesterId, error)
            }
            throw error
        }
    }
}
