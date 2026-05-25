package com.kdongsu5509.notifications.adapter.out.persistence

import com.kdongsu5509.notifications.domain.NotificationHistory
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import java.util.*

@ExtendWith(MockitoExtension::class)
class NotificationHistoryPersistenceAdapterTest {

    @Mock
    private lateinit var mapper: NotificationHistoryMapper

    @Mock
    private lateinit var repository: SpringDataNotificationHistoryRepository

    @InjectMocks
    private lateinit var adapter: NotificationHistoryPersistenceAdapter

    @BeforeEach
    fun setUp() {
        adapter = NotificationHistoryPersistenceAdapter(repository, mapper)
    }

    @Test
    @DisplayName("알림 내역을 저장한다")
    fun save_success() {
        // given
        val domain = NotificationHistory(
            receiverEmail = "test@ex.com",
            senderNickname = "sender",
            title = "title",
            body = "body",
            type = "type",
            path = null,
            isRead = false
        )
        val entity = NotificationHistoryJpaEntity(
            receiverEmail = "test@ex.com",
            senderNickname = "sender",
            title = "title",
            body = "body",
            type = "type",
            path = null,
            isRead = false
        )
        val savedEntity = NotificationHistoryJpaEntity(
            id = 1L,
            receiverEmail = "test@ex.com",
            senderNickname = "sender",
            title = "title",
            body = "body",
            type = "type",
            path = null,
            isRead = false
        )
        val savedDomain = NotificationHistory(
            id = 1L,
            receiverEmail = "test@ex.com",
            senderNickname = "sender",
            title = "title",
            body = "body",
            type = "type",
            path = null,
            isRead = false
        )

        `when`(mapper.toEntity(domain)).thenReturn(entity)
        `when`(repository.save(entity)).thenReturn(savedEntity)
        `when`(mapper.toDomain(savedEntity)).thenReturn(savedDomain)

        // when
        val result = adapter.save(domain)

        // then
        assertThat(result).isEqualTo(savedDomain)
        verify(mapper).toEntity(domain)
        verify(repository).save(entity)
        verify(mapper).toDomain(savedEntity)
    }

    @Test
    @DisplayName("ID로 조회 시 엔티티가 존재하면 도메인으로 반환한다")
    fun findById_success() {
        // given
        val id = 1L
        val entity = NotificationHistoryJpaEntity(
            id = 1L,
            receiverEmail = "test@ex.com",
            senderNickname = "sender",
            title = "title",
            body = "body",
            type = "type",
            path = null,
            isRead = false
        )
        val domain = NotificationHistory(
            id = 1L,
            receiverEmail = "test@ex.com",
            senderNickname = "sender",
            title = "title",
            body = "body",
            type = "type",
            path = null,
            isRead = false
        )

        `when`(repository.findById(id)).thenReturn(Optional.of(entity))
        `when`(mapper.toDomain(entity)).thenReturn(domain)

        // when
        val result = adapter.findById(id)

        // then
        assertThat(result).isEqualTo(domain)
    }

    @Test
    @DisplayName("ID로 조회 시 엔티티가 없으면 null을 반환한다")
    fun findById_returnsNull() {
        // given
        val id = 1L
        `when`(repository.findById(id)).thenReturn(Optional.empty())

        // when
        val result = adapter.findById(id)

        // then
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("수신자 이메일로 페이징 조회한다")
    fun findByReceiverEmail_success() {
        // given
        val receiverEmail = "test@ex.com"
        val page = 0
        val size = 20
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))

        val entity = NotificationHistoryJpaEntity(
            id = 1L,
            receiverEmail = receiverEmail,
            senderNickname = "sender",
            title = "title",
            body = "body",
            type = "type",
            path = null,
            isRead = false
        )
        val domain = NotificationHistory(
            id = 1L,
            receiverEmail = receiverEmail,
            senderNickname = "sender",
            title = "title",
            body = "body",
            type = "type",
            path = null,
            isRead = false
        )
        val pageResult = PageImpl(listOf(entity))

        `when`(repository.findByReceiverEmailOrderByCreatedAtDesc(receiverEmail, pageable)).thenReturn(pageResult)
        `when`(mapper.toDomain(entity)).thenReturn(domain)

        // when
        val result = adapter.findByReceiverEmail(receiverEmail, page, size)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(domain)
    }
}
