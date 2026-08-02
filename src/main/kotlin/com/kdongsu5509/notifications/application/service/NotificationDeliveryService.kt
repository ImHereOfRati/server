package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.adapter.out.firebase.RetryableFcmException
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.out.ExternalMessagePort
import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.application.port.out.FirebasePort
import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.application.port.out.SenderAliasPort
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationStatus
import com.kdongsu5509.notifications.domain.NotificationTemplate
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.domain.SMS
import com.kdongsu5509.notifications.event.NotificationDeliveryFailed
import com.kdongsu5509.notifications.event.NotificationEvent
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.notifications.exception.UnregisteredTokenException
import com.kdongsu5509.shared.event.DomainEventPublisher
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.support.exception.type.InvalidInputException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.retry.annotation.Backoff
import org.springframework.retry.annotation.Recover
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDateTime
import java.util.UUID

@Service
class NotificationDeliveryService(
    private val persistencePort: NotificationPersistencePort,
    private val senderAliasPort: SenderAliasPort,
    private val firebasePort: FirebasePort,
    private val fcmTokenPersistencePort: FcmTokenPersistencePort,
    private val externalMessagePort: ExternalMessagePort,
    private val eventPublisher: DomainEventPublisher,
    transactionManager: PlatformTransactionManager,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val newTransaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    // --- 발송 ---------------------------------------------------------------

    @Retryable(
        retryFor = [RetryableFcmException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1_000, multiplier = 2.0, maxDelay = 8_000),
    )
    fun deliver(request: NotificationEvent) {
        val notification = try {
            reserve(request)
        } catch (_: DataIntegrityViolationException) {
            return
        } ?: return

        attempt(notification, request.senderId)
    }

    @Retryable(
        retryFor = [RetryableFcmException::class],
        maxAttempts = 3,
        backoff = Backoff(delay = 1_000, multiplier = 2.0, maxDelay = 8_000),
    )
    fun redeliver(notificationId: Long) {
        val notification = findById(notificationId)
        if (!notification.isDeliverable) return
        attempt(notification, requesterId = null)
    }

    @Recover
    fun recover(error: RetryableFcmException, request: NotificationEvent) {
        val notification = inNewTransaction {
            persistencePort.findByDedupeKey(
                Notification.dedupeKeyOf(request.eventId, request.notificationMethod)
            )
        } ?: return
        notifyFailure(notification, request.senderId, error)
    }

    @Recover
    fun recover(error: RetryableFcmException, notificationId: Long) {
        notifyFailure(findById(notificationId), requesterId = null, error = error)
    }

    private fun attempt(notification: Notification, requesterId: UUID?) {
        val id = requireNotNull(notification.id) { "저장되지 않은 알림은 발송할 수 없습니다." }

        try {
            send(notification)
            inNewTransaction { persistencePort.save(findById(id).markSent(LocalDateTime.now())) }
            notifySuccess(notification, requesterId)
        } catch (error: Exception) {
            val failed = inNewTransaction {
                persistencePort.save(
                    findById(id).markFailed(error.message ?: error.javaClass.simpleName)
                )
            }
            if (error !is RetryableFcmException) {
                notifyFailure(failed, requesterId, error)
            }
            throw error
        }
    }

    // --- 접수 ---------------------------------------------------------------

    private fun reserve(request: NotificationEvent): Notification? = inNewTransaction {
        val dedupeKey = Notification.dedupeKeyOf(request.eventId, request.notificationMethod)
        persistencePort.findByDedupeKey(dedupeKey)?.let { existing ->
            return@inNewTransaction existing.takeIf { it.status == NotificationStatus.FAILED }
        }

        val rendered = NotificationTemplate.render(
            type = request.type,
            senderAlias = resolveSenderAlias(request),
            extraData = request.extraData,
        )
        val bodyOverride = if (request.notificationMethod == NotificationMethod.SMS) {
            request.extraData[NotificationCommand.BODY_KEY]
                ?.takeIf(String::isNotBlank)
                ?: throw InvalidInputException("SMS 본문이 누락되었습니다.")
        } else {
            null
        }

        persistencePort.save(
            Notification.request(
                dedupeKey = dedupeKey,
                targetIdentifier = request.targetIdentifier,
                method = request.notificationMethod,
                rendered = rendered,
                bodyOverride = bodyOverride,
            )
        )
    }

    private fun resolveSenderAlias(request: NotificationEvent): String {
        if (request.type in NICKNAME_ONLY_TYPES) return request.senderNickname

        // SMS는 대상을 전화번호로 지목하므로 사용자로 풀리지 않는다. 별칭도 있을 수 없다.
        val ownerId = request.targetIdentifier.toUuidOrNull() ?: return request.senderNickname

        return runCatching { senderAliasPort.findAlias(ownerId = ownerId, senderId = request.senderId) }
            .onFailure { log.warn("발송자 별칭 조회 실패. 닉네임으로 대체한다.", it) }
            .getOrNull()
            ?: request.senderNickname
    }

    // --- 외부 채널 ----------------------------------------------------------

    private fun send(notification: Notification) = when (notification.method) {
        NotificationMethod.FCM -> sendFcm(notification)
        NotificationMethod.SMS -> sendSms(notification)
    }

    private fun sendFcm(notification: Notification) {
        val receiverId = notification.targetIdentifier.toUuidOrNull()
            ?: throw InvalidInputException(
                "FCM 알림의 수신자는 사용자 식별자여야 합니다.",
                contextData = mapOf("targetIdentifier" to notification.targetIdentifier),
            )

        val token = fcmTokenPersistencePort.findByOwnerId(receiverId)
            ?: NotificationException.FCM_TOKEN_NOT_FOUND.throwIt(
                contextData = mapOf("receiverId" to receiverId),
            )

        try {
            firebasePort.send(token.fcmToken, token.deviceType, notification.toRendered())
        } catch (exception: UnregisteredTokenException) {
            token.id?.let(fcmTokenPersistencePort::deleteById)
            throw exception
        }
    }

    private fun sendSms(notification: Notification) {
        val result = externalMessagePort.send(
            SMS(
                senderNickname = notification.senderAlias,
                receiverNumber = notification.targetIdentifier,
                body = notification.body,
            )
        )
        if (!result.isSuccess) {
            NotificationException.SMS_SEND_FAILED.throwIt(
                contextData = mapOf(
                    "receiverNumber" to notification.targetIdentifier,
                    "status" to result.status,
                    "message" to result.message,
                )
            )
        }
    }

    // --- 결과 통지 ----------------------------------------------------------

    private fun notifySuccess(notification: Notification, requesterId: UUID?) {
        runCatching { publishReceipt(notification, requesterId, NotificationType.DELIVERY_RESULT_NOTICE) }
            .onFailure { log.error("알림 발송 성공 통지 중 오류", it) }
    }

    private fun notifyFailure(notification: Notification, requesterId: UUID?, error: Throwable) {
        runCatching {
            publishDeliveryFailure(notification, error)
            publishReceipt(notification, requesterId, NotificationType.DELIVERY_FAILED_NOTICE)
        }.onFailure { log.error("알림 발송 실패 통지 중 오류", it) }
    }

    private fun publishDeliveryFailure(notification: Notification, error: Throwable) {
        inNewTransaction {
            eventPublisher.publish(
                NotificationDeliveryFailed(
                    notificationId = notification.id,
                    targetIdentifier = notification.targetIdentifier,
                    notificationType = notification.type.name,
                    errorType = error.javaClass.simpleName,
                    errorMessage = error.message,
                )
            )
        }
    }

    private fun publishReceipt(notification: Notification, requesterId: UUID?, type: NotificationType) {
        if (notification.type.isMeta) return
        if (requesterId == null) return
        inNewTransaction { eventPublisher.publish(NotificationEvent.deliveryReceipt(requesterId, type)) }
    }

    // --- 공통 ---------------------------------------------------------------

    private fun findById(id: Long): Notification =
        inNewTransaction { persistencePort.findById(id) }
            ?: NotificationException.NOTIFICATION_NOT_FOUND.throwIt(contextData = mapOf("id" to id))

    @Suppress("UNCHECKED_CAST")
    private fun <T> inNewTransaction(block: () -> T): T =
        newTransaction.execute { block() } as T

    private fun String.toUuidOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()

    private companion object {
        val NICKNAME_ONLY_TYPES = setOf(
            NotificationType.FRIEND_REQUEST_RECEIVED,
            NotificationType.FRIEND_REQUEST_ACCEPTED,
        )
    }
}
