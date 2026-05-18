package com.kdongsu5509.auth.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "security")
class SecurityWhiteList(
    val whitelist: List<String> = emptyList()
)
