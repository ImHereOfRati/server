package com.kdongsu5509.support.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "firebase")
data class FcmProperties(
    var path: String = ""
)
