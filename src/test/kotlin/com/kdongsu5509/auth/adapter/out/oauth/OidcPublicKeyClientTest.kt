package com.kdongsu5509.auth.adapter.out.oauth

import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKeyResponse
import com.kdongsu5509.auth.application.port.out.CachePort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.time.Duration
import org.springframework.web.client.RestClient

@ExtendWith(MockitoExtension::class)
class OidcPublicKeyClientTest {

    @Mock
    private lateinit var oidcPublicKeyApiClient: OidcPublicKeyApiClient

    @Mock
    private lateinit var restClientBuilder: RestClient.Builder

    @Mock
    private lateinit var cachePort: CachePort

    private lateinit var client: OidcPublicKeyClient

    @BeforeEach
    fun setUp() {
        client = OidcPublicKeyClient(oidcPublicKeyApiClient, restClientBuilder, cachePort)
    }

    @Test
    @DisplayName("fetch 호출 시 캐시에 값이 없으면 ApiClient를 통해 OIDCPublicKeyResponse를 가져와 캐싱한다")
    fun fetchWhenCacheEmpty() {
        val mockResponse = OIDCPublicKeyResponse(keys = emptyList())
        val key = "kakaoOidcKeys::kakaoPublicKeySet"
        whenever(cachePort.find(key, OIDCPublicKeyResponse::class.java)).thenReturn(null)
        whenever(oidcPublicKeyApiClient.fetchPublicKey()).thenReturn(mockResponse)

        val result = client.fetch(key, "https://kauth.kakao.com/.well-known/jwks.json")

        assertThat(result).isSameAs(mockResponse)
        verify(cachePort).find(key, OIDCPublicKeyResponse::class.java)
        verify(oidcPublicKeyApiClient).fetchPublicKey()
        verify(cachePort).save(key, mockResponse, Duration.ofDays(8))
    }

    @Test
    @DisplayName("fetch 호출 시 캐시에 값이 있으면 ApiClient를 호출하지 않고 캐시 값을 반환한다")
    fun fetchWhenCachePresent() {
        val mockResponse = OIDCPublicKeyResponse(keys = emptyList())
        val key = "kakaoOidcKeys::kakaoPublicKeySet"
        whenever(cachePort.find(key, OIDCPublicKeyResponse::class.java)).thenReturn(mockResponse)

        val result = client.fetch(key, "https://kauth.kakao.com/.well-known/jwks.json")

        assertThat(result).isSameAs(mockResponse)
        verify(cachePort).find(key, OIDCPublicKeyResponse::class.java)
        verifyNoInteractions(oidcPublicKeyApiClient)
    }

    @Test
    @DisplayName("refresh 호출 시 ApiClient를 통해 새로운 OIDCPublicKeyResponse를 가져와 캐싱한다")
    fun refresh() {
        val mockResponse = OIDCPublicKeyResponse(keys = emptyList())
        val key = "kakaoOidcKeys::kakaoPublicKeySet"
        whenever(oidcPublicKeyApiClient.fetchPublicKey()).thenReturn(mockResponse)

        val result = client.refresh(key, "https://kauth.kakao.com/.well-known/jwks.json")

        assertThat(result).isSameAs(mockResponse)
        verify(oidcPublicKeyApiClient).fetchPublicKey()
        verify(cachePort).save(key, mockResponse, Duration.ofDays(8))
    }
}
