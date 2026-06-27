package com.kdongsu5509.auth.security.handler

import com.kdongsu5509.support.external.DiscordMessageSender
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.ott.OneTimeToken
import org.springframework.security.authentication.ott.DefaultOneTimeToken
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class ImHereOttSuccessHandlerTest {

    @Mock
    private lateinit var discordMessageSender: DiscordMessageSender

    private val webhookUrl = "http://discord.webhook"
    private lateinit var handler: ImHereOttSuccessHandler

    @BeforeEach
    fun setUp() {
        handler = ImHereOttSuccessHandler(discordMessageSender, webhookUrl)
    }

    @Test
    @DisplayName("handle 호출 시 Discord 메시지를 전송하고 OTT 입력 페이지로 이동시킨다")
    fun handle() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val token: OneTimeToken = DefaultOneTimeToken("token-value", "admin", Instant.now().plusSeconds(60))

        handler.handle(request, response, token)

        verify(discordMessageSender).sendMessage(eq(webhookUrl), any())
        assert(response.status == 302)
        assert(response.redirectedUrl == "/admin/ott?username=admin")
    }

    @Test
    @DisplayName("같은 관리자가 윈도우 내 4회째 요청하면 429로 차단한다")
    fun rateLimit() {
        repeat(3) {
            val request = MockHttpServletRequest()
            val response = MockHttpServletResponse()
            val token: OneTimeToken = DefaultOneTimeToken("token-$it", "admin", Instant.now().plusSeconds(60))
            handler.handle(request, response, token)
            assert(response.status == 302)
        }

        val response = MockHttpServletResponse()
        val token: OneTimeToken = DefaultOneTimeToken("token-4", "admin", Instant.now().plusSeconds(60))
        handler.handle(MockHttpServletRequest(), response, token)

        assert(response.status == 429)
    }
}
