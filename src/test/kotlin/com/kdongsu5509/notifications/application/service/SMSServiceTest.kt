package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.port.out.ExternalMessagePort
import com.kdongsu5509.notifications.domain.SMS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify

@ExtendWith(MockitoExtension::class)
class SMSServiceTest {

    @Mock
    private lateinit var externalMessagePort: ExternalMessagePort

    private lateinit var service: SMSService

    @BeforeEach
    fun setUp() {
        service = SMSService(externalMessagePort)
    }

    @Test
    @DisplayName("단건 SMS를 전송한다")
    fun send_success() {
        service.send(
            senderNickname = "sender",
            receiverNumber = "01012345678",
            location = "Seoul"
        )

        val captor = argumentCaptor<SMS>()
        verify(externalMessagePort).send(captor.capture())
        
        val sms = captor.firstValue
        assertThat(sms.senderNickname).isEqualTo("sender")
        assertThat(sms.receiverNumber).isEqualTo("01012345678")
        assertThat(sms.location).isEqualTo("Seoul")
    }

    @Test
    @DisplayName("다건 SMS를 전송한다")
    fun sendMultiple_success() {
        service.sendMultiple(
            senderNickname = "sender",
            receiverNumbers = listOf("01012345678", "01087654321"),
            location = "Seoul"
        )

        val captor = argumentCaptor<List<SMS>>()
        verify(externalMessagePort).sendMultiple(captor.capture())
        
        val smsList = captor.firstValue
        assertThat(smsList).hasSize(2)
        assertThat(smsList.map { it.receiverNumber })
            .containsExactly("01012345678", "01087654321")
    }
}
