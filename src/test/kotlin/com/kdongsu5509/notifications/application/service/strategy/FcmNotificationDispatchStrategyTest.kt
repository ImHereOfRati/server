package com.kdongsu5509.notifications.application.service.strategy

import com.kdongsu5509.notifications.application.dto.MultipleNotificationCommand
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.NotificationUseCase
import com.kdongsu5509.notifications.domain.NotificationMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class FcmNotificationDispatchStrategyTest {

    @Mock
    private lateinit var fcmNotificationService: NotificationUseCase

    private lateinit var strategy: FcmNotificationDispatchStrategy

    @BeforeEach
    fun setUp() {
        strategy = FcmNotificationDispatchStrategy(fcmNotificationService)
    }

    @Test
    @DisplayName("NotificationMethod가 FCM인지 확인한다")
    fun checkNotificationMethod() {
        assertThat(strategy.notificationMethod).isEqualTo(NotificationMethod.FCM)
    }

    @Test
    @DisplayName("단건 커맨드를 FCM 서비스로 디스패치한다")
    fun dispatch() {
        val command = NotificationCommand(
            senderNickname = "sender",
            senderEmail = "sender@example.com",
            notificationMethod = NotificationMethod.FCM,
            targetIdentifier = "receiver@example.com",
            type = "TYPE",
            extraData = mapOf("key" to "value")
        )

        strategy.dispatch(command)

        verify(fcmNotificationService).send(
            senderNickname = "sender",
            senderEmail = "sender@example.com",
            receiverEmail = "receiver@example.com",
            type = "TYPE",
            extraData = mapOf("key" to "value")
        )
    }

    @Test
    @DisplayName("다건 커맨드를 FCM 서비스로 각각 디스패치한다")
    fun dispatchMultiple() {
        val command = MultipleNotificationCommand(
            senderNickname = "sender",
            senderEmail = "sender@example.com",
            notificationMethod = NotificationMethod.FCM,
            targetIdentifiers = listOf("receiver1@example.com", "receiver2@example.com"),
            type = "TYPE",
            extraData = mapOf("key" to "value")
        )

        strategy.dispatchMultiple(command)

        verify(fcmNotificationService).send(
            senderNickname = "sender",
            senderEmail = "sender@example.com",
            receiverEmail = "receiver1@example.com",
            type = "TYPE",
            extraData = mapOf("key" to "value")
        )
        verify(fcmNotificationService).send(
            senderNickname = "sender",
            senderEmail = "sender@example.com",
            receiverEmail = "receiver2@example.com",
            type = "TYPE",
            extraData = mapOf("key" to "value")
        )
    }
}
