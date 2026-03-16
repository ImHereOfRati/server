package com.kdongsu5509.imhere.common.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "firebase")
data class FcmProperties(
    var path: String = ""
)