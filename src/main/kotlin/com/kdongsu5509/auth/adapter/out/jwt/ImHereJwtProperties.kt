package com.kdongsu5509.auth.adapter.out.jwt

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "jwt")
data class ImHereJwtProperties(
    var secret: String = "",

    var accessExpirationMinutes: Long = 0,

    var refreshExpirationDays: Long = 0,

    var adminExpirationMinutes: Long = 0,

    var accessHeaderName: String = "",
)
