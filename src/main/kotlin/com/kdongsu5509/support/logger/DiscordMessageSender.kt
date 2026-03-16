package com.kdongsu5509.support.logger

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class DiscordMessageSender : MessageSendPort {

    private val logger = LoggerFactory.getLogger(DiscordMessageSender::class.java)
    private val restTemplate = RestTemplate()

    @Value("\${discord.url:}")
    private val specificWebhookUrl: String? = null

    override fun sendMessage(content: String) {
        if (specificWebhookUrl.isNullOrEmpty()) {
            logger.warn("Discord webhook URL not configured. Skipping message send.")
            return
        }

        try {
            val message = ErrorAlertMessage(content)
            sendToDiscord(message)
            logger.info("디스코드 알림 전송 성공: {}", content)
        } catch (e: Exception) {
            logger.error("디스코드 알림 전송 실패", e)
        }
    }

    private fun sendToDiscord(message: ErrorAlertMessage) {
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val entity = HttpEntity(message, headers)

        restTemplate.postForEntity(specificWebhookUrl!!, entity, Void::class.java)
    }
}
