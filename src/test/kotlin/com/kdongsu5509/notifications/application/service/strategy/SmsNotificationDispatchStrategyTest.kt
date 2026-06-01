package com.kdongsu5509.notifications.application.service.strategy

import com.kdongsu5509.notifications.application.dto.MultipleNotificationCommand
import com.kdongsu5509.notifications.application.dto.NotificationCommand
import com.kdongsu5509.notifications.application.port.`in`.MessageSendUseCase
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
class SmsNotificationDispatchStrategyTest {

    @Mock
    private lateinit var smsService: MessageSendUseCase

    private lateinit var strategy: SmsNotificationDispatchStrategy

    @BeforeEach
    fun setUp() {
        strategy = SmsNotificationDispatchStrategy(smsService)
    }

    @Test
    @DisplayName("NotificationMethod가 PHONE_NUMBER인지 확인한다")
    fun checkNotificationMethod() {
        assertThat(strategy.notificationMethod).isEqualTo(NotificationMethod.PHONE_NUMBER)
    }

    @Test
    @DisplayName("단건 커맨드를 SMS 서비스로 디스패치한다")
    fun dispatch() {
        val command = NotificationCommand(
            senderNickname = "sender",
            senderEmail = "sender@example.com",
            notificationMethod = NotificationMethod.PHONE_NUMBER,
            targetIdentifier = "01012345678",
            type = "TYPE",
            extraData = mapOf("location" to "Seoul")
        )

        strategy.dispatch(command)

        verify(smsService).send(
            senderNickname = "sender",
            receiverNumber = "01012345678",
            location = "Seoul"
        )
    }

    @Test
    @DisplayName("다건 커맨드를 SMS 서비스로 디스패치한다")
    fun dispatchMultiple() {
        val command = MultipleNotificationCommand(
            senderNickname = "sender",
            senderEmail = "sender@example.com",
            notificationMethod = NotificationMethod.PHONE_NUMBER,
            targetIdentifiers = listOf("01012345678", "01087654321"),
            type = "TYPE",
            extraData = mapOf("location" to "Seoul")
        )

        strategy.dispatchMultiple(command)

        verify(smsService).sendMultiple(
            senderNickname = "sender",
            receiverNumbers = listOf("01012345678", "01087654321"),
            location = "Seoul"
        )
    }
}
