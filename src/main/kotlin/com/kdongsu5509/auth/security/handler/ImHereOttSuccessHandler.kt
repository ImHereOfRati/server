package com.kdongsu5509.auth.security.handler

import com.kdongsu5509.shared.response.APIResponseSerializers
import com.kdongsu5509.support.external.DiscordMessageDto
import com.kdongsu5509.support.external.DiscordMessageSender
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.ott.OneTimeToken
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
class ImHereOttSuccessHandler(
    private val discordMessageSender: DiscordMessageSender,
    @param:Value("\${discord.url.ott}") private val ottAlertChannelWebhookUrl: String
) : OneTimeTokenGenerationSuccessHandler {

    companion object {
        private const val SUCCESS_MESSAGE = "OTT를 정상적으로 발급하였습니다."
        private const val DISCORD_MESSAGE_TEMPLATE = """
            ### 🔐 ImHere 관리자 OTT 로그인 요청
            - **요청 관리자**: `%s`
            - **OTT 토큰**: `%s`

            > 해당 토큰을 사용하여 로그인을 완료해주세요.
        """
    }

    override fun handle(request: HttpServletRequest, response: HttpServletResponse, oneTimeToken: OneTimeToken) {
        val message = DiscordMessageDto(createOTTMessage(oneTimeToken))
        discordMessageSender.sendMessage(ottAlertChannelWebhookUrl, message)
        response.sendRedirect("/admin/ott?username=${URLEncoder.encode(oneTimeToken.username, StandardCharsets.UTF_8)}")
    }

    private fun createOTTMessage(oneTimeToken: OneTimeToken): String {
        val message = DISCORD_MESSAGE_TEMPLATE
            .trimIndent()
            .format(oneTimeToken.username, oneTimeToken.tokenValue)
        return message
    }
}
