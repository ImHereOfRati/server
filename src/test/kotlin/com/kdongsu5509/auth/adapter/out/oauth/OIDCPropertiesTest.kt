package com.kdongsu5509.auth.adapter.out.oauth

import com.kdongsu5509.user.domain.OAuth2Provider
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
                    issuers = mutableListOf("issuer"),
                    audiences = mutableListOf("aud"),
                    cacheKey = "key",
                    jwksUri = "uri"
                )
            )
        )

        assertThat(properties.get(OAuth2Provider.KAKAO).cacheKey).isEqualTo("key")
        assertThat(properties.get(OAuth2Provider.KAKAO).issuers).containsExactly("issuer")
        assertThat(properties.get(OAuth2Provider.KAKAO).audiences).containsExactly("aud")
        assertThat(properties.configuredProviders()).containsExactly(OAuth2Provider.KAKAO)
    }

    @Test
    @DisplayName("issuer와 audience는 여러 값을 그대로 들고 있는다")
    fun get_keepsMultipleIssuersAndAudiences() {
        val properties = OIDCProperties(
            providers = mutableMapOf(
                "google" to OIDCProperties.Provider(
                    issuers = mutableListOf("https://accounts.google.com", "accounts.google.com"),
                    audiences = mutableListOf("web-client-id", "ios-client-id"),
                    cacheKey = "g",
                    jwksUri = "uri"
                )
            )
        )

        val config = properties.get(OAuth2Provider.GOOGLE)

        assertThat(config.issuers).containsExactly("https://accounts.google.com", "accounts.google.com")
        assertThat(config.audiences).containsExactly("web-client-id", "ios-client-id")
    }

    @Test
    @DisplayName("환경변수를 비워 둔 audience는 걸러 낸다")
    fun get_dropsBlankAudiences() {
        // given: 쓰지 않는 플랫폼의 client ID를 빈 문자열로 남겨 둔 배포를 흉내 낸다.
        val properties = OIDCProperties(
            providers = mutableMapOf(
                "apple" to OIDCProperties.Provider(
                    issuers = mutableListOf("https://appleid.apple.com", ""),
                    audiences = mutableListOf("bundle-id", "", "   "),
                    cacheKey = "a",
                    jwksUri = "uri"
                )
            )
        )

        val config = properties.get(OAuth2Provider.APPLE)

        assertThat(config.issuers).containsExactly("https://appleid.apple.com")
        assertThat(config.audiences).containsExactly("bundle-id")
    }

    @Test
    @DisplayName("여러 provider가 설정되면 OAuth2Provider.entries 순서로 모두 반환한다")
    fun configuredProviders_returnsAllInEntriesOrder() {
        val properties = OIDCProperties(
            providers = mutableMapOf(
                "apple" to OIDCProperties.Provider(cacheKey = "a"),
                "google" to OIDCProperties.Provider(cacheKey = "g"),
                "kakao" to OIDCProperties.Provider(cacheKey = "k")
            )
        )

        // 삽입 순서(apple, google, kakao)와 무관하게 enum 선언 순서(KAKAO, GOOGLE, APPLE)로 반환
        assertThat(properties.configuredProviders())
            .containsExactly(OAuth2Provider.KAKAO, OAuth2Provider.GOOGLE, OAuth2Provider.APPLE)
        assertThat(properties.get(OAuth2Provider.GOOGLE).cacheKey).isEqualTo("g")
        assertThat(properties.get(OAuth2Provider.KAKAO).cacheKey).isEqualTo("k")
        assertThat(properties.get(OAuth2Provider.APPLE).cacheKey).isEqualTo("a")
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
