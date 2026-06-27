package com.kdongsu5509.auth.security.handler

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.kdongsu5509.auth.security.ClientIpResolver
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
import java.util.concurrent.TimeUnit

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
        private const val MAX_OTT_REQUESTS = 3
    }

    // Caffeine expireAfterWrite로 rate limit 윈도우(5분) 만료를 위임한다.
    // 수동 시간 비교 불필요, eviction 자동.
    private val ottRequestTracker: Cache<String, OttRequestInfo> =
        Caffeine.newBuilder()
            .expireAfterWrite(OTT_VALIDITY_MINUTES, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build()

    override fun handle(request: HttpServletRequest, response: HttpServletResponse, oneTimeToken: OneTimeToken) {
        val clientIp = ClientIpResolver.resolve(request)

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
        // 만료는 캐시가 처리하므로, 존재하면 윈도우 내 요청이다.
        val requestInfo = ottRequestTracker.getIfPresent(username)

        if (requestInfo == null) {
            ottRequestTracker.put(username, OttRequestInfo(clientIp, 1))
            return true
        }

        if (requestInfo.requestCount >= MAX_OTT_REQUESTS) {
            return false
        }

        ottRequestTracker.put(username, requestInfo.copy(requestCount = requestInfo.requestCount + 1))
        return true
    }

    private fun createOTTMessage(oneTimeToken: OneTimeToken, clientIp: String): String {
        val message = DISCORD_MESSAGE_TEMPLATE
            .trimIndent()
            .format(oneTimeToken.username, clientIp, Instant.now(), oneTimeToken.tokenValue)
        return message
    }

    private data class OttRequestInfo(
        val clientIp: String,
        val requestCount: Int
    )
}
