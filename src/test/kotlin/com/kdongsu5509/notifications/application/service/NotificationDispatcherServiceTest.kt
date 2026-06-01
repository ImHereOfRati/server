package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.dto.MultipleNotificationCommand
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.service.strategy.NotificationDispatchStrategy
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.exception.NotificationException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class NotificationDispatcherServiceTest {

    @Mock
    private lateinit var fcmStrategy: NotificationDispatchStrategy

    @Mock
    private lateinit var smsStrategy: NotificationDispatchStrategy

    private lateinit var service: NotificationDispatcherService

    @BeforeEach
    fun setUp() {
        whenever(fcmStrategy.notificationMethod).thenReturn(NotificationMethod.FCM)
        whenever(smsStrategy.notificationMethod).thenReturn(NotificationMethod.PHONE_NUMBER)

        service = NotificationDispatcherService(listOf(fcmStrategy, smsStrategy))
    }

    @Test
    @DisplayName("지원하는 단건 커맨드를 올바른 전략으로 디스패치한다")
    fun dispatch_success() {
        val command = NotificationCommand(
            senderNickname = "sender",
            senderEmail = "sender@example.com",
            notificationMethod = NotificationMethod.FCM,
            targetIdentifier = "target@example.com",
            type = "TYPE",
            extraData = emptyMap()
        )

        service.dispatch(command)

        verify(fcmStrategy).dispatch(command)
        verify(smsStrategy, never()).dispatch(any())
    }

    @Test
    @DisplayName("지원하지 않는 단건 커맨드 디스패치 시 예외가 발생한다")
    fun dispatch_unsupported() {
        val emptyService = NotificationDispatcherService(emptyList())
        val command = NotificationCommand(
            senderNickname = "sender",
            senderEmail = "sender@example.com",
            notificationMethod = NotificationMethod.FCM,
            targetIdentifier = "target@example.com",
            type = "TYPE",
            extraData = emptyMap()
        )

        assertThatThrownBy { emptyService.dispatch(command) }
            .isInstanceOf(RuntimeException::class.java)
    }

    @Test
    @DisplayName("지원하는 다건 커맨드를 올바른 전략으로 디스패치한다")
    fun dispatchMultiple_success() {
        val command = MultipleNotificationCommand(
            senderNickname = "sender",
            senderEmail = "sender@example.com",
            notificationMethod = NotificationMethod.PHONE_NUMBER,
            targetIdentifiers = listOf("01011112222"),
            type = "TYPE",
            extraData = emptyMap()
        )

        service.dispatchMultiple(command)

        verify(smsStrategy).dispatchMultiple(command)
        verify(fcmStrategy, never()).dispatchMultiple(any())
    }

    @Test
    @DisplayName("지원하지 않는 다건 커맨드 디스패치 시 예외가 발생한다")
    fun dispatchMultiple_unsupported() {
        val emptyService = NotificationDispatcherService(emptyList())
        val command = MultipleNotificationCommand(
            senderNickname = "sender",
            senderEmail = "sender@example.com",
            notificationMethod = NotificationMethod.FCM,
            targetIdentifiers = listOf("target@example.com"),
            type = "TYPE",
            extraData = emptyMap()
        )

        assertThatThrownBy { emptyService.dispatchMultiple(command) }
            .isInstanceOf(RuntimeException::class.java)
    }
}
