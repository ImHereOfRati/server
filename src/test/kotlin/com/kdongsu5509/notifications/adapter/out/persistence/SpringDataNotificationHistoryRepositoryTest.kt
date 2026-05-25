package com.kdongsu5509.notifications.adapter.out.persistence

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
class SpringDataNotificationHistoryRepositoryTest {

    @Autowired
    private lateinit var repository: SpringDataNotificationHistoryRepository

    @Test
    @DisplayName("수신자 이메일로 알림 내역을 페이징 조회한다")
    fun findByReceiverEmailOrderByCreatedAtDesc_success() {
        // given
        val email = "test@ex.com"
        val entity1 = NotificationHistoryJpaEntity(
            receiverEmail = email,
            senderNickname = "sender1",
            title = "title1",
            body = "body1",
            type = "type1",
            path = null,
            isRead = false
        )
        val entity2 = NotificationHistoryJpaEntity(
            receiverEmail = email,
            senderNickname = "sender2",
            title = "title2",
            body = "body2",
            type = "type2",
            path = null,
            isRead = false
        )
        repository.save(entity1)
        repository.save(entity2)

        // when
        val pageable = PageRequest.of(0, 10)
        val result = repository.findByReceiverEmailOrderByCreatedAtDesc(email, pageable)

        // then
        assertThat(result.content).hasSize(2)
        assertThat(result.content).extracting("receiverEmail").containsOnly(email)
    }
}
