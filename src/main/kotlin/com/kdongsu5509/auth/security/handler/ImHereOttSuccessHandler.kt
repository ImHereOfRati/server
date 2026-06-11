package com.kdongsu5509.auth.security.handler

import com.kdongsu5509.shared.response.APIResponseSerializers
import com.kdongsu5509.support.external.DiscordMessageDto
import com.kdongsu5509.support.external.DiscordMessageSender
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.ott.OneTimeToken
import org.springframework.security.web.authentication.ott.OneTimeTokenGenerationSuccessHandler
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

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
            - **요청 IP**: `%s`
            - **요청 시각**: `%s`
            - **OTT 토큰**: `%s`

            > 해당 토큰을 사용하여 로그인을 완료해주세요.
        """
        private const val OTT_VALIDITY_MINUTES = 5L
    }

    private val ottRequestTracker = ConcurrentHashMap<String, OttRequestInfo>()

    override fun handle(request: HttpServletRequest, response: HttpServletResponse, oneTimeToken: OneTimeToken) {
        val clientIp = extractClientIp(request)

        if (!canIssueOtt(oneTimeToken.username, clientIp)) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            response.writer.write("""{"error": "OTT 요청이 너무 많습니다. 잠시 후 다시 시도하세요."}""")
            return
        }

        val message = DiscordMessageDto(createOTTMessage(oneTimeToken, clientIp))
        discordMessageSender.sendMessage(ottAlertChannelWebhookUrl, message)
        response.sendRedirect("/admin/ott?username=${URLEncoder.encode(oneTimeToken.username, StandardCharsets.UTF_8)}")
    }

    private fun canIssueOtt(username: String, clientIp: String): Boolean {
        val now = Instant.now().toEpochMilli()
        val requestInfo = ottRequestTracker[username]

        if (requestInfo == null) {
            ottRequestTracker[username] = OttRequestInfo(now, clientIp, 1)
            return true
        }

        if (now - requestInfo.lastRequestTime > OTT_VALIDITY_MINUTES * 60 * 1000) {
            ottRequestTracker[username] = OttRequestInfo(now, clientIp, 1)
            return true
        }

        if (requestInfo.requestCount >= 3) {
            return false
        }

        ottRequestTracker[username] = requestInfo.copy(requestCount = requestInfo.requestCount + 1)
        return true
    }

    private fun extractClientIp(request: HttpServletRequest): String {
        val xForwardedFor = request.getHeader("X-Forwarded-For")
        if (!xForwardedFor.isNullOrEmpty() && !xForwardedFor.contains("unknown")) {
            return xForwardedFor.split(",")[0].trim()
        }

        val xRealIp = request.getHeader("X-Real-IP")
        if (!xRealIp.isNullOrEmpty() && !xRealIp.contains("unknown")) {
            return xRealIp
        }

        return request.remoteAddr
    }

    private fun createOTTMessage(oneTimeToken: OneTimeToken, clientIp: String): String {
        val message = DISCORD_MESSAGE_TEMPLATE
            .trimIndent()
            .format(oneTimeToken.username, clientIp, Instant.now(), oneTimeToken.tokenValue)
        return message
    }

    private data class OttRequestInfo(
        val lastRequestTime: Long,
        val clientIp: String,
        val requestCount: Int
    )
}
