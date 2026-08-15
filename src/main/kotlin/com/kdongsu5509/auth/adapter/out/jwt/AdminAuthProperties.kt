package com.kdongsu5509.auth.adapter.out.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "admin.mobile")
data class AdminAuthProperties(
    var passwordHash: String = "",
    var totpSecret: String = "",
    var challengeExpirationSeconds: Long = 300,
    var refreshExpirationDays: Long = 7,
)
