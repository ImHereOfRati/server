package com.kdongsu5509.support.external

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DiscordUserErrorNotifier(
    private val discordMessageSendPort: DiscordMessageSendPort,
    @param:Value("\${discord.url.usererror:}") private val userErrorWebhookUrl: String
) {

    fun notifyUserError(request: HttpServletRequest, errorCode: String, errorMessage: String) {
        if (userErrorWebhookUrl.isEmpty()) return

        val content = """
            ## ⚠️ User Error (4xx)
            **Type:** Business / Input Error
            **Code:** `$errorCode`
            **Message:** $errorMessage
            **${request.method}** `${resolveUri(request)}`
            **IP:** ${resolveClientIp(request)}
            **User:** ${resolveUser(request)}
        """.trimIndent()

        discordMessageSendPort.sendMessage(userErrorWebhookUrl, DiscordMessageDto(content))
    }

    fun notifyAbnormalAccess(request: HttpServletRequest, errorCode: String, errorMessage: String) {
        if (userErrorWebhookUrl.isEmpty()) return

        val content = """
            ## 🚨 Abnormal Access (403)
            **Type:** Authorization Denied
            **Code:** `$errorCode`
            **Message:** $errorMessage
            **${request.method}** `${resolveUri(request)}`
            **IP:** ${resolveClientIp(request)}
            **User:** ${resolveUser(request)}
        """.trimIndent()

        discordMessageSendPort.sendMessage(userErrorWebhookUrl, DiscordMessageDto(content))
    }

    private fun resolveUri(request: HttpServletRequest): String {
        val query = request.queryString
        return if (query.isNullOrEmpty()) request.requestURI else "${request.requestURI}?$query"
    }

    private fun resolveClientIp(request: HttpServletRequest): String {
        return request.getHeader("X-Forwarded-For")?.split(",")?.first()?.trim()
            ?: request.remoteAddr
    }

    private fun resolveUser(request: HttpServletRequest): String {
        return request.userPrincipal?.name ?: "anonymous"
    }
}
