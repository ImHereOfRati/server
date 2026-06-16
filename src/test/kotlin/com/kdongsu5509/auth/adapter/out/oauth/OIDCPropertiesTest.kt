package com.kdongsu5509.auth.adapter.out.oauth

import com.kdongsu5509.auth.domain.OAuth2Provider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class OIDCPropertiesTest {

    @Test
    @DisplayName("providers 맵에서 설정을 읽는다")
    fun get_fromProvidersMap() {
        val properties = OIDCProperties(
            providers = mutableMapOf(
                "kakao" to OIDCProperties.Provider(
                    issuer = "issuer",
                    audience = "aud",
                    cacheKey = "key",
                    jwksUri = "uri"
                )
            )
        )

        assertThat(properties.get(OAuth2Provider.KAKAO).cacheKey).isEqualTo("key")
        assertThat(properties.configuredProviders()).containsExactly(OAuth2Provider.KAKAO)
    }
}
