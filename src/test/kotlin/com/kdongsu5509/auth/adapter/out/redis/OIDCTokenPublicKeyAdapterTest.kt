package com.kdongsu5509.auth.adapter.out.redis

import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKey
import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKeyResponse
import com.kdongsu5509.auth.application.port.out.OauthClientPort
import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.auth.AuthException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class OIDCTokenPublicKeyAdapterTest {

    @Mock
    private lateinit var oauthClientPort: OauthClientPort

    private lateinit var adapter: OIDCTokenPublicKeyAdapter

    @BeforeEach
    fun setUp() {
        adapter = OIDCTokenPublicKeyAdapter(oauthClientPort)
    }

    @Test
    @DisplayName("캐시된 키 목록에 일치하는 kid가 있으면 반환한다")
    fun findByKeyId_fromCache() {
        val key = OIDCPublicKey("kid1", "kty", "alg", "use", "n", "e")
        val response = OIDCPublicKeyResponse(listOf(key))
        whenever(oauthClientPort.fetch()).thenReturn(response)

        val result = adapter.findByKeyId("kid1")

        assertThat(result.kid).isEqualTo("kid1")
    }

    @Test
    @DisplayName("캐시된 키가 없으면 KAKAO_OIDC_PUBLIC_KEY_FETCH_FROM_REDIS_FAILED 예외를 발생시킨다")
    fun findByKeyId_cacheFetchFail() {
        whenever(oauthClientPort.fetch()).thenReturn(null)

        assertThatThrownBy { adapter.findByKeyId("kid1") }
            .isInstanceOf(ImHereBaseException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", AuthException.KAKAO_OIDC_PUBLIC_KEY_FETCH_FROM_REDIS_FAILED)
    }

    @Test
    @DisplayName("캐시된 목록에 일치하는 kid가 없으면 refresh 후 조회하여 반환한다")
    fun findByKeyId_fromRefresh() {
        val oldKey = OIDCPublicKey("kid2", "kty", "alg", "use", "n", "e")
        val oldResponse = OIDCPublicKeyResponse(listOf(oldKey))
        whenever(oauthClientPort.fetch()).thenReturn(oldResponse)

        val newKey = OIDCPublicKey("kid1", "kty", "alg", "use", "n", "e")
        val newResponse = OIDCPublicKeyResponse(listOf(oldKey, newKey))
        whenever(oauthClientPort.refresh()).thenReturn(newResponse)

        val result = adapter.findByKeyId("kid1")

        assertThat(result.kid).isEqualTo("kid1")
    }

    @Test
    @DisplayName("refresh 후에도 없으면 KAKAO_OIDC_PUBLIC_KEY_NOT_FOUND 예외를 발생시킨다")
    fun findByKeyId_refreshNotFound() {
        val oldKey = OIDCPublicKey("kid2", "kty", "alg", "use", "n", "e")
        val oldResponse = OIDCPublicKeyResponse(listOf(oldKey))
        whenever(oauthClientPort.fetch()).thenReturn(oldResponse)
        whenever(oauthClientPort.refresh()).thenReturn(oldResponse)

        assertThatThrownBy { adapter.findByKeyId("kid1") }
            .isInstanceOf(ImHereBaseException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", AuthException.KAKAO_OIDC_PUBLIC_KEY_NOT_FOUND)
    }

    @Test
    @DisplayName("refresh 결과가 없으면 KAKAO_OIDC_PUBLIC_KEY_FETCH_FAILED 예외를 발생시킨다")
    fun findByKeyId_refreshFail() {
        val oldKey = OIDCPublicKey("kid2", "kty", "alg", "use", "n", "e")
        val oldResponse = OIDCPublicKeyResponse(listOf(oldKey))
        whenever(oauthClientPort.fetch()).thenReturn(oldResponse)
        whenever(oauthClientPort.refresh()).thenReturn(null)

        assertThatThrownBy { adapter.findByKeyId("kid1") }
            .isInstanceOf(ImHereBaseException::class.java)
            .hasFieldOrPropertyWithValue("errorCode", AuthException.KAKAO_OIDC_PUBLIC_KEY_FETCH_FAILED)
    }
}
