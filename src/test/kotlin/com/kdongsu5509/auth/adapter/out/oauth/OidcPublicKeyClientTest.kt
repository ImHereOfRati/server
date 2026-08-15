package com.kdongsu5509.auth.adapter.out.oauth

import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKeyResponse
import com.kdongsu5509.shared.cache.CachePort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import org.springframework.web.client.RestClient
import java.time.Duration

@ExtendWith(MockitoExtension::class)
class OidcPublicKeyClientTest {

    @Mock private lateinit var restClientBuilder: RestClient.Builder
    @Mock private lateinit var restClient: RestClient
    @Mock private lateinit var request: RestClient.RequestHeadersUriSpec<*>
    @Mock private lateinit var response: RestClient.ResponseSpec
    @Mock private lateinit var cachePort: CachePort

    private lateinit var client: OidcPublicKeyClient

    @BeforeEach
    fun setUp() {
        client = OidcPublicKeyClient(restClientBuilder, cachePort)
    }

    @Test
    fun fetch_uses_configured_jwks_uri_for_any_provider() {
        val key = "provider-jwks"
        val uri = "https://example.com/.well-known/jwks.json"
        val result = OIDCPublicKeyResponse(keys = emptyList())
        whenever(cachePort.find(key, OIDCPublicKeyResponse::class.java)).thenReturn(null)
        whenever(restClientBuilder.build()).thenReturn(restClient)
        whenever(restClient.get()).thenReturn(request)
        whenever(request.uri(uri)).thenReturn(request)
        whenever(request.retrieve()).thenReturn(response)
        whenever(response.body(OIDCPublicKeyResponse::class.java)).thenReturn(result)

        assertThat(client.fetch(key, uri)).isSameAs(result)
        verify(request).uri(uri)
        verify(cachePort).save(key, result, Duration.ofDays(8))
    }

    @Test
    fun fetch_returns_cached_value_without_remote_call() {
        val key = "provider-jwks"
        val result = OIDCPublicKeyResponse(keys = emptyList())
        whenever(cachePort.find(key, OIDCPublicKeyResponse::class.java)).thenReturn(result)

        assertThat(client.fetch(key, "https://example.com/keys")).isSameAs(result)
    }
}
