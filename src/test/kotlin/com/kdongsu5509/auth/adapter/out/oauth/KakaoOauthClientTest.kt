package com.kdongsu5509.auth.adapter.out.oauth

import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKeyResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class KakaoOauthClientTest {

    @Mock
    private lateinit var kakaoOauthPublicKeyApiClient: KakaoOauthPublicKeyApiClient

    private lateinit var client: KakaoOauthClient

    @BeforeEach
    fun setUp() {
        client = KakaoOauthClient(kakaoOauthPublicKeyApiClient)
    }

    @Test
    @DisplayName("fetch 호출 시 ApiClient를 통해 OIDCPublicKeyResponse를 가져온다")
    fun fetch() {
        val mockResponse = OIDCPublicKeyResponse(keys = emptyList())
        whenever(kakaoOauthPublicKeyApiClient.fetchKakaoPublicKey()).thenReturn(mockResponse)

        val result = client.fetch()

        assertThat(result).isSameAs(mockResponse)
        verify(kakaoOauthPublicKeyApiClient).fetchKakaoPublicKey()
    }

    @Test
    @DisplayName("refresh 호출 시 ApiClient를 통해 새로운 OIDCPublicKeyResponse를 가져온다")
    fun refresh() {
        val mockResponse = OIDCPublicKeyResponse(keys = emptyList())
        whenever(kakaoOauthPublicKeyApiClient.fetchKakaoPublicKey()).thenReturn(mockResponse)

        val result = client.refresh()

        assertThat(result).isSameAs(mockResponse)
        verify(kakaoOauthPublicKeyApiClient).fetchKakaoPublicKey()
    }
}
