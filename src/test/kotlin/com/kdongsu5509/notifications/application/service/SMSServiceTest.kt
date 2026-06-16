package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.adapter.out.solapi.SolapiResponse
import com.kdongsu5509.notifications.application.port.out.ExternalMessagePort
import com.kdongsu5509.notifications.domain.SMS
import com.kdongsu5509.support.exception.type.InvalidInputException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

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
        whenever(externalMessagePort.send(any())).thenReturn(SolapiResponse.success())

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
        whenever(externalMessagePort.sendMultiple(any())).thenReturn(
            listOf(SolapiResponse.success(), SolapiResponse.success())
        )

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

    @Test
    @DisplayName("렌더링된 SMS 본문이 45자를 초과하면 전송을 거부한다")
    fun send_rejectsTooLongBody() {
        assertThatThrownBy {
            service.send(
                senderNickname = "very-long-sender-name",
                receiverNumber = "01012345678",
                location = "very-long-location-name-that-breaks-limit"
            )
        }.isInstanceOf(InvalidInputException::class.java)

        verifyNoInteractions(externalMessagePort)
    }

    @Test
    @DisplayName("수신자 번호가 비면 전송을 거부한다")
    fun send_rejectsEmptyReceiver() {
        assertThatThrownBy {
            service.send(
                senderNickname = "sender",
                receiverNumber = "",
                location = "Seoul"
            )
        }.isInstanceOf(InvalidInputException::class.java)

        verifyNoInteractions(externalMessagePort)
    }
}
