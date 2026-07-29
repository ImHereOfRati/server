package com.kdongsu5509.notifications.adapter.`in`.event

import com.common.testsupport.PersistenceTestSupport
import com.kdongsu5509.friends.event.FriendRequestSent
import com.kdongsu5509.notifications.adapter.out.persistence.SpringDataFcmTokenRepository
import com.kdongsu5509.notifications.adapter.out.persistence.SpringDataNotificationRepository
import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.FcmToken
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationStatus
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
        saveToken(event.receiverEmail)
        saveToken(event.requesterEmail)

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
        saveToken(event.receiverEmail)
        saveToken(event.requesterEmail)

        inTransaction { eventPublisher.publish(event) }
        inTransaction { eventPublisher.publish(event) }

        await().atMost(Duration.ofSeconds(5)).untilAsserted {
            assertThat(find(event)?.status).isEqualTo(NotificationStatus.SENT)
            assertThat(notificationRepository.countByDedupeKey(dedupeKey(event))).isEqualTo(1)
            assertThat(publicationCount()).isZero()
        }
    }

    private fun event() = FriendRequestSent(
        requesterEmail = "sender-${UUID.randomUUID()}@example.com",
        requesterNickname = "보낸이",
        receiverEmail = "receiver-${UUID.randomUUID()}@example.com",
    )

    private fun saveToken(email: String) =
        fcmTokenPersistencePort.save(FcmToken.create(email, "token-$email", DeviceType.AOS))

    private fun find(event: FriendRequestSent): Notification? =
        persistencePort.findByDedupeKey(dedupeKey(event))

    private fun dedupeKey(event: FriendRequestSent): String =
        Notification.dedupeKeyOf(event.eventId, NotificationMethod.FCM)

    private fun inTransaction(block: () -> Unit) =
        TransactionTemplate(transactionManager).executeWithoutResult { block() }

    private fun publicationCount(): Long =
        jdbcTemplate.queryForObject("select count(*) from event_publication", Long::class.java)!!
}
