package com.kdongsu5509.auth.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "security")
class SecurityWhiteList(
    var whitelist: List<String> = emptyList(),
    var corsAllowedOrigins: List<String> = emptyList()
)
