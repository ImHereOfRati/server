package com.kdongsu5509.notifications.adapter.`in`.event

import com.common.testsupport.PersistenceTestSupport
import com.kdongsu5509.friends.event.FriendRequestSent
import com.kdongsu5509.notifications.adapter.out.persistence.SpringDataFcmTokenRepository
import com.kdongsu5509.notifications.adapter.out.persistence.SpringDataNotificationRepository
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.NotificationUseCase
import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.FcmToken
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationStatus
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.shared.event.DomainEventPublisher
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.util.UUID

@Transactional(propagation = Propagation.NOT_SUPPORTED)
class FriendNotificationEventIntegrationTest : PersistenceTestSupport() {
    @Autowired
    private lateinit var eventPublisher: DomainEventPublisher

    @Autowired
    private lateinit var notificationUseCase: NotificationUseCase

    @Autowired
    private lateinit var persistencePort: NotificationPersistencePort

    @Autowired
    private lateinit var fcmTokenPersistencePort: FcmTokenPersistencePort

    @Autowired
    private lateinit var notificationRepository: SpringDataNotificationRepository

    @Autowired
    private lateinit var fcmTokenRepository: SpringDataFcmTokenRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun clean() {
        jdbcTemplate.execute("alter table event_publication alter column serialized_event varchar(4000)")
        jdbcTemplate.update("delete from event_publication")
        notificationRepository.deleteAll()
        fcmTokenRepository.deleteAll()
    }

    @Test
    @DisplayName("친구 요청 이벤트는 커밋 후 비동기 발송되어 SENT로 기록된다")
    fun event_is_delivered_after_commit() {
        val event = event()
        saveToken(event.receiverId)
        saveToken(event.requesterId)

        inTransaction { eventPublisher.publish(event) }

        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertThat(find(event)?.status).isEqualTo(NotificationStatus.SENT)
            assertThat(publicationCount()).isZero()
        }
    }

    @Test
    @DisplayName("발행 트랜잭션이 롤백되면 알림과 이벤트 발행 기록이 남지 않는다")
    fun rollback_does_not_deliver() {
        val event = event()
        runCatching {
            inTransaction {
                eventPublisher.publish(event)
                error("rollback")
            }
        }

        assertThat(find(event)).isNull()
        assertThat(publicationCount()).isZero()
    }

    @Test
    @DisplayName("같은 이벤트가 두 번 처리되어도 dedupe_key로 알림은 하나만 예약된다")
    fun duplicate_event_is_suppressed() {
        val event = event()
        saveToken(event.receiverId)
        saveToken(event.requesterId)

        inTransaction { eventPublisher.publish(event) }
        inTransaction { eventPublisher.publish(event) }

        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertThat(find(event)?.status).isEqualTo(NotificationStatus.SENT)
            assertThat(notificationRepository.countByDedupeKey(dedupeKey(event))).isEqualTo(1)
            assertThat(publicationCount()).isZero()
        }
    }

    // NotificationUseCase.request가 트랜잭션 경계를 열어 주지 않으면 @ApplicationModuleListener가
    // 깨어나지 않아 알림이 아예 접수되지 않는다. 발행 여부가 아니라 "실제로 배달됐는지"를 본다.
    @Test
    @DisplayName("발송 요청 유스케이스는 트랜잭션 안에서 발행하므로 리스너까지 도달한다")
    fun request_use_case_opens_transaction_so_listener_runs() {
        val receiverId = UUID.randomUUID()
        val senderId = UUID.randomUUID()
        saveToken(receiverId)
        saveToken(senderId)

        notificationUseCase.requestDelivery(
            NotificationCommand(
                senderNickname = "보낸이",
                senderId = senderId,
                notificationMethod = NotificationMethod.FCM,
                targetIdentifiers = listOf(receiverId.toString()),
                type = NotificationType.FRIEND_REQUEST_RECEIVED,
            )
        )

        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            val delivered = notificationRepository.findAll()
                .filter { it.targetIdentifier == receiverId.toString() }
            assertThat(delivered).hasSize(1)
            assertThat(delivered.first().status).isEqualTo(NotificationStatus.SENT)
            assertThat(publicationCount()).isZero()
        }
    }

    private fun event() = FriendRequestSent(
        requesterId = UUID.randomUUID(),
        requesterNickname = "보낸이",
        receiverId = UUID.randomUUID(),
    )

    private fun saveToken(ownerId: UUID) =
        fcmTokenPersistencePort.save(
            FcmToken(ownerId = ownerId, fcmToken = "token-$ownerId", deviceType = DeviceType.AOS)
        )

    private fun find(event: FriendRequestSent): Notification? =
        persistencePort.findByDedupeKey(dedupeKey(event))

    private fun dedupeKey(event: FriendRequestSent): String =
        Notification.dedupeKeyOf(event.eventId, NotificationMethod.FCM)

    private fun inTransaction(block: () -> Unit) =
        TransactionTemplate(transactionManager).executeWithoutResult { block() }

    private fun publicationCount(): Long =
        jdbcTemplate.queryForObject("select count(*) from event_publication", Long::class.java)!!
}
