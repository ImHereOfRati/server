package com.kdongsu5509.support.config

import com.solapi.sdk.SolapiClient
import com.solapi.sdk.message.service.DefaultMessageService
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(ExternalSMSProperties::class)
class ExternalSMSConfig(private val properties: ExternalSMSProperties) {
    @Bean
    fun defaultMessageService(): DefaultMessageService {
        return SolapiClient.createInstance(
            properties.apiKey,
            properties.apiSecret
        )
    }
}
