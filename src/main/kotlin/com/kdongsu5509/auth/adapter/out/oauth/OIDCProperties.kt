package com.kdongsu5509.auth.adapter.out.oauth

import com.kdongsu5509.auth.domain.OAuth2Provider
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "oidc")
data class OIDCProperties(
    var kakao: Provider = Provider(),
    var google: Provider = Provider()
) {
    data class Provider(
        var issuer: String = "",
        var audience: String = "",
        var cacheKey: String = "",
        var jwksUri: String = ""
    )

    fun get(provider: OAuth2Provider): Provider = when (provider) {
        OAuth2Provider.KAKAO -> kakao
        OAuth2Provider.GOOGLE -> google
    }
}
