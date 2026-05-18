package com.kdongsu5509.auth.adapter.out.oauth

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "oidc.kakao")
data class KakaoOIDCProperties(
    var issuer: String = "",
    var audience: String = "",
    var cacheKey: String = ""
)
