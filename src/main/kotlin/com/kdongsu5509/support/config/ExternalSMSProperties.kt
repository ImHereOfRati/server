package com.kdongsu5509.support.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "solapi")
data class ExternalSMSProperties(
    val sender: String = "",
    val apiKey: String = "",
    val apiSecret: String = ""
)
