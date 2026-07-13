package com.kdongsu5509.support.external

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DiscordUserErrorNotifier(
    private val discordMessageSendPort: DiscordMessageSendPort,
    @param:Value("\${discord.url.error.client:}") private val userErrorWebhookUrl: String
) {

    fun notifyUserError(request: HttpServletRequest, errorCode: String, errorMessage: String) {
        if (userErrorWebhookUrl.isEmpty()) return

        val alert = DiscordAlertMessage.userError(errorCode, errorMessage, RequestContext.from(request))
        discordMessageSendPort.sendMessage(userErrorWebhookUrl, alert.toDto())
    }

    fun notifyAbnormalAccess(request: HttpServletRequest, errorCode: String, errorMessage: String) {
        if (userErrorWebhookUrl.isEmpty()) return

        val alert = DiscordAlertMessage.abnormalAccess(errorCode, errorMessage, RequestContext.from(request))
        discordMessageSendPort.sendMessage(userErrorWebhookUrl, alert.toDto())
    }
}
