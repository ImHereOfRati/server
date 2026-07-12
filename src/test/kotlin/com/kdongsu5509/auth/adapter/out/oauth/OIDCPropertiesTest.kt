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

    @Test
    @DisplayName("여러 provider가 설정되면 OAuth2Provider.entries 순서로 모두 반환한다")
    fun configuredProviders_returnsAllInEntriesOrder() {
        val properties = OIDCProperties(
            providers = mutableMapOf(
                "google" to OIDCProperties.Provider(cacheKey = "g"),
                "kakao" to OIDCProperties.Provider(cacheKey = "k")
            )
        )

        // 삽입 순서(google, kakao)와 무관하게 enum 선언 순서(KAKAO, GOOGLE)로 반환
        assertThat(properties.configuredProviders())
            .containsExactly(OAuth2Provider.KAKAO, OAuth2Provider.GOOGLE)
        assertThat(properties.get(OAuth2Provider.GOOGLE).cacheKey).isEqualTo("g")
        assertThat(properties.get(OAuth2Provider.KAKAO).cacheKey).isEqualTo("k")
    }

    @Test
    @DisplayName("설정되지 않은 provider 조회는 에러를 던진다")
    fun get_missingProvider_throws() {
        val properties = OIDCProperties(providers = mutableMapOf())

        org.junit.jupiter.api.assertThrows<IllegalStateException> {
            properties.get(OAuth2Provider.KAKAO)
        }
    }
}
