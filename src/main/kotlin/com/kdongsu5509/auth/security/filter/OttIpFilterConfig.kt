package com.kdongsu5509.auth.security.filter

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "admin")
data class OttIpFilterConfig(
    var id: String = "",
    var nickname: String = "",
    var allowedIps: List<String> = emptyList()
)
