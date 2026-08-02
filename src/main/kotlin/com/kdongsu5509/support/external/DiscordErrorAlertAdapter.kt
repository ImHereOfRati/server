package com.kdongsu5509.support.external

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import java.net.URI

@Component
class DiscordErrorAlertAdapter(
    private val discordApiClient: DiscordApiClient,
    @param:Value("\${discord.url.error.client:}") private val clientErrorWebhookUrl: String,
    @param:Value("\${discord.url.error.server:}") private val serverErrorWebhookUrl: String,
    @param:Value("\${discord.url.ott:}") private val ottWebhookUrl: String,
) : ErrorAlertPort {

    private val logger = LoggerFactory.getLogger(DiscordErrorAlertAdapter::class.java)

    @Async("discordExecutor")
    override fun send(channel: AlertChannel, message: AlertMessage) {
        val webhookUrl = webhookOf(channel)
        if (webhookUrl.isEmpty()) {
            logger.warn("Discord webhook URL not configured for {}. Skipping alert.", channel)
            return
        }

        try {
            discordApiClient.sendMessage(URI.create(webhookUrl), DiscordMessageDto(message.content))
            logger.info("디스코드 경보 전송 성공: {}", channel)
        } catch (e: Exception) {
            logger.error("디스코드 경보 전송 실패: {}", channel, e)
        }
    }

    private fun webhookOf(channel: AlertChannel): String = when (channel) {
        AlertChannel.CLIENT_ERROR -> clientErrorWebhookUrl
        AlertChannel.SERVER_ERROR -> serverErrorWebhookUrl
        AlertChannel.ADMIN_OTT -> ottWebhookUrl
    }
}
