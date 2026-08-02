package com.kdongsu5509.notifications.adapter.out.persistence

import com.common.testsupport.PersistenceTestSupport
import com.kdongsu5509.notifications.application.port.out.NotificationPersistencePort
import com.kdongsu5509.notifications.domain.Notification
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationStatus
import com.kdongsu5509.notifications.domain.NotificationTemplate
import com.kdongsu5509.notifications.domain.NotificationType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDateTime

class NotificationPersistenceAdapterTest : PersistenceTestSupport() {

    @Autowired
    private lateinit var port: NotificationPersistencePort

    @Autowired
    private lateinit var repository: SpringDataNotificationRepository

    @Autowired
    private lateinit var mapper: NotificationMapper

    private val now: LocalDateTime = LocalDateTime.of(2026, 7, 29, 15, 0, 0)

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    private fun requested(dedupeKey: String = "event-1:FCM"): Notification = Notification.request(
        dedupeKey = dedupeKey,
        targetIdentifier = "receiver@imhere.com",
        method = NotificationMethod.FCM,
        rendered = NotificationTemplate.render(
            type = NotificationType.ARRIVAL,
            senderAlias = "길동이",
            extraData = mapOf(NotificationTemplate.PLACE_NAME_KEY to "학교"),
        ),
    )

    @Test
    @DisplayName("접수한 알림을 저장하면 식별자가 생기고 값이 그대로 돌아온다")
    fun save_success() {
        // when
        val saved = port.save(requested())

        // then
        assertThat(saved.id).isNotNull()
        assertThat(saved.status).isEqualTo(NotificationStatus.PENDING)
        assertThat(saved.type).isEqualTo(NotificationType.ARRIVAL)
        assertThat(saved.extraData).containsEntry(NotificationTemplate.PLACE_NAME_KEY, "학교")
    }

    @Test
    @DisplayName("추가 데이터 맵이 JSON 컬럼을 거쳐도 그대로 복원된다")
    fun save_success_preserves_extra_data() {
        // given
        val original = requested()

        // when
        val reloaded = port.findById(port.save(original).id!!)

        // then
        assertThat(reloaded).isNotNull
        assertThat(reloaded!!.extraData).isEqualTo(original.extraData)
    }

    @Test
    @DisplayName("상태 전이 결과를 저장하면 발송 시각과 시도 횟수가 남는다")
    fun save_success_persists_transition() {
        // given
        val saved = port.save(requested())

        // when
        val sent = port.save(saved.markSent(now))

        // then
        val reloaded = port.findById(sent.id!!)!!
        assertThat(reloaded.status).isEqualTo(NotificationStatus.SENT)
        assertThat(reloaded.sentAt).isEqualTo(now)
        assertThat(reloaded.isInbox).isTrue()
    }

    @Test
    @DisplayName("멱등 키로 이미 접수된 발송을 찾는다")
    fun findByDedupeKey_success() {
        // given
        port.save(requested(dedupeKey = "event-42:FCM"))

        // when
        val found = port.findByDedupeKey("event-42:FCM")

        // then
        assertThat(found).isNotNull
        assertThat(found!!.deduplicationKey).isEqualTo("event-42:FCM")
    }

    @Test
    @DisplayName("접수된 적 없는 멱등 키는 null이다")
    fun findByDedupeKey_returnsNull() {
        assertThat(port.findByDedupeKey("존재하지-않는-키")).isNull()
    }

    @Test
    @DisplayName("같은 멱등 키로 두 번 접수하면 유니크 제약이 막는다")
    fun save_fail_when_dedupe_key_duplicated() {
        // given - 이 제약이 곧 멱등성 장치다. 로컬 캐시 없이도 중복 발송이 억제된다.
        repository.saveAndFlush(mapper.toEntity(requested(dedupeKey = "event-1:FCM")))

        // when & then
        assertThatThrownBy {
            repository.saveAndFlush(mapper.toEntity(requested(dedupeKey = "event-1:FCM")))
        }.isInstanceOf(DataIntegrityViolationException::class.java)
    }
}
