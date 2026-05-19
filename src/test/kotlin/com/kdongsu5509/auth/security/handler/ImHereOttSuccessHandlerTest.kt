package com.kdongsu5509.auth.security.handler

import com.kdongsu5509.support.external.DiscordMessageDto
import com.kdongsu5509.support.external.DiscordMessageSender
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.argumentCaptor
import org.springframework.security.authentication.ott.OneTimeToken
import java.io.PrintWriter
import java.io.StringWriter

@ExtendWith(MockitoExtension::class)
class ImHereOttSuccessHandlerTest {

    @Mock
    private lateinit var discordMessageSender: DiscordMessageSender

    @Mock
    private lateinit var request: HttpServletRequest

    @Mock
    private lateinit var response: HttpServletResponse

    @Mock
    private lateinit var oneTimeToken: OneTimeToken

    private val webhookUrl = "https://discord.com/api/webhooks/test-ott"

    private lateinit var handler: ImHereOttSuccessHandler

    @BeforeEach
    fun setUp() {
        handler = ImHereOttSuccessHandler(discordMessageSender, webhookUrl)
    }

    @Test
    @DisplayName("OTT 발급 시 토큰 정보가 담긴 Discord 메시지를 올바른 webhook URL로 전송한다")
    fun handle_success_sendsDiscordMessage() {
        val writer = PrintWriter(StringWriter())
        `when`(oneTimeToken.tokenValue).thenReturn("test-token-value")
        `when`(oneTimeToken.username).thenReturn("admin@example.com")
        `when`(response.writer).thenReturn(writer)

        handler.handle(request, response, oneTimeToken)

        val messageCaptor = argumentCaptor<DiscordMessageDto>()
        val urlCaptor = argumentCaptor<String>()
        verify(discordMessageSender).sendMessage(urlCaptor.capture(), messageCaptor.capture())

        assertThat(urlCaptor.firstValue).isEqualTo(webhookUrl)
        assertThat(messageCaptor.firstValue.content).contains("test-token-value")
        assertThat(messageCaptor.firstValue.content).contains("admin@example.com")
    }

    @Test
    @DisplayName("OTT 발급 성공 시 응답 status는 200, content-type은 text/plain으로 설정한다")
    fun handle_success_configuresResponse() {
        val writer = PrintWriter(StringWriter())
        `when`(oneTimeToken.tokenValue).thenReturn("any-token")
        `when`(oneTimeToken.username).thenReturn("admin@example.com")
        `when`(response.writer).thenReturn(writer)

        handler.handle(request, response, oneTimeToken)

        verify(response).status = HttpServletResponse.SC_OK
        verify(response).contentType = "application/json"
        verify(response).characterEncoding = "UTF-8"
    }
}
