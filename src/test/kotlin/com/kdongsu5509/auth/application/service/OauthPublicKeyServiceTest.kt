package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.adapter.out.oauth.OIDCProperties
import com.kdongsu5509.auth.application.port.out.OauthClientPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class OauthPublicKeyServiceTest {

    @Mock
    private lateinit var oauthClientPort: OauthClientPort

    private lateinit var oidcProperties: OIDCProperties

    private lateinit var publicKeyService: OauthPublicKeyService

    @BeforeEach
    fun setUp() {
        oidcProperties = OIDCProperties(
            providers = mutableMapOf(
                "kakao" to OIDCProperties.Provider(
                    issuer = "https://kauth.kakao.com",
                    audience = "kakao-client-id",
                    cacheKey = "kakao-cache",
                    jwksUri = "https://kauth.kakao.com/.well-known/jwks.json"
                ),
                "google" to OIDCProperties.Provider(
                    issuer = "https://accounts.google.com",
                    audience = "google-client-id",
                    cacheKey = "google-cache",
                    jwksUri = "https://www.googleapis.com/oauth2/v3/certs"
                )
            )
        )
        publicKeyService = OauthPublicKeyService(oauthClientPort, oidcProperties)
    }

    @Test
    @DisplayName("OAuth 공개키를 외부 API로부터 성공적으로 강제 갱신한다")
    fun fetch_success() {
        // when
        publicKeyService.fetch(OAuth2Provider.KAKAO)

        // then
        then(oauthClientPort).should().refresh("kakao-cache", "https://kauth.kakao.com/.well-known/jwks.json")
    }

    @Test
    @DisplayName("등록된 모든 OIDC 공개키를 외부 API로부터 강제 갱신한다")
    fun fetchAll_success() {
        // when
        publicKeyService.fetchAll()

        // then
        then(oauthClientPort).should().refresh("kakao-cache", "https://kauth.kakao.com/.well-known/jwks.json")
        then(oauthClientPort).should().refresh("google-cache", "https://www.googleapis.com/oauth2/v3/certs")
    }
}
