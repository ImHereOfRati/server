package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.port.out.NotificationHistoryPersistencePort
import com.kdongsu5509.notifications.domain.NotificationHistory
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.type.ForbiddenException
import com.kdongsu5509.support.exception.type.NotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class NotificationHistoryServiceTest {

    @Mock
    private lateinit var persistencePort: NotificationHistoryPersistencePort

    private lateinit var service: NotificationHistoryService

    @BeforeEach
    fun setUp() {
        service = NotificationHistoryService(persistencePort)
    }

    @Test
    @DisplayName("수신자 이메일로 알림 내역을 조회한다")
    fun findByReceiverEmail_success() {
        // given
        val email = "test@ex.com"
        val page = 0
        val size = 20
        val history = NotificationHistory(
            id = 1L,
            receiverEmail = email,
            senderNickname = "sender",
            title = "title",
            body = "body",
            type = "type",
            path = null,
            isRead = false,
            createdAt = LocalDateTime.now()
        )
        whenever(persistencePort.findByReceiverEmail(email, page, size)).thenReturn(listOf(history))

        // when
        val result = service.findByReceiverEmail(email, page, size)

        // then
        assertThat(result).hasSize(1)
        assertThat(result[0]).isEqualTo(history)
        verify(persistencePort).findByReceiverEmail(email, page, size)
    }

    @Test
    @DisplayName("알림을 정상적으로 읽음 처리한다")
    fun markAsRead_success() {
        // given
        val email = "test@ex.com"
        val id = 1L
        val history = NotificationHistory(
            id = 1L,
            receiverEmail = email,
            senderNickname = "sender",
            title = "title",
            body = "body",
            type = "type",
            path = null,
            isRead = false,
            createdAt = LocalDateTime.now()
        )
        whenever(persistencePort.findById(id)).thenReturn(history)

        // when
        service.markAsRead(email, id)

        // then
        val captor = argumentCaptor<NotificationHistory>()
        verify(persistencePort).save(captor.capture())
        assertThat(captor.firstValue.isRead).isTrue()
    }

    @Test
    @DisplayName("알림이 존재하지 않으면 예외가 발생한다")
    fun markAsRead_notFound() {
        // given
        val email = "test@ex.com"
        val id = 1L
        whenever(persistencePort.findById(id)).thenReturn(null)

        // when & then
        assertThatThrownBy { service.markAsRead(email, id) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessage(NotificationException.NOTIFICATION_NOT_FOUND.errorMessage)
    }

    @Test
    @DisplayName("자신의 알림이 아니면 읽음 처리 시 예외가 발생한다")
    fun markAsRead_notMyNotification() {
        // given
        val email = "test@ex.com"
        val id = 1L
        val history = NotificationHistory(
            id = 1L,
            receiverEmail = "other@ex.com",
            senderNickname = "sender",
            title = "title",
            body = "body",
            type = "type",
            path = null,
            isRead = false,
            createdAt = LocalDateTime.now()
        )
        whenever(persistencePort.findById(id)).thenReturn(history)

        // when & then
        assertThatThrownBy { service.markAsRead(email, id) }
            .isInstanceOf(ForbiddenException::class.java)
            .hasMessage(NotificationException.NOT_MY_NOTIFICATION.errorMessage)
    }
}
