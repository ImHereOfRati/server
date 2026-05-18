package com.kdongsu5509.auth.adapter.out.redis

import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKey
import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKeyResponse
import com.kdongsu5509.auth.application.port.out.OauthClientPort
import com.kdongsu5509.support.exception.type.InternalServerException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import org.mockito.kotlin.then

@ExtendWith(MockitoExtension::class)
class OIDCTokenPublicKeyAdapterTest {

    @Mock
    private lateinit var oauthClientPort: OauthClientPort

    private lateinit var OIDCTokenPublicKeyAdapter: OIDCTokenPublicKeyAdapter

    @BeforeEach
    fun setUp() {
        OIDCTokenPublicKeyAdapter = OIDCTokenPublicKeyAdapter(oauthClientPort)
    }

    @Test
    @DisplayName("kid가 일치하는 공개키가 캐시에 있으면 성공적으로 반환한다")
    fun findByKeyId_success() {
        // given
        val kid = "test-kid"
        val expectedKey = OIDCPublicKey(kid = kid, n = "modulus", e = "exponent")
        val response = OIDCPublicKeyResponse(keys = listOf(expectedKey))

        given(oauthClientPort.fetch()).willReturn(response)

        // when
        val result = OIDCTokenPublicKeyAdapter.findByKeyId(kid)

        // then
        assertThat(result.kid).isEqualTo(kid)
        assertThat(result.n).isEqualTo("modulus")
        assertThat(result.e).isEqualTo("exponent")
    }

    @Test
    @DisplayName("kid가 일치하는 공개키가 캐시에 없으면 갱신을 시도하고 성공하면 반환한다")
    fun findByKeyId_refresh_success() {
        // given
        val kid = "new-kid"
        val expectedKey = OIDCPublicKey(kid = kid, n = "modulus", e = "exponent")
        val initialResponse = OIDCPublicKeyResponse(keys = listOf(OIDCPublicKey(kid = "old-kid")))
        val refreshedResponse = OIDCPublicKeyResponse(keys = listOf(expectedKey))

        given(oauthClientPort.fetch()).willReturn(initialResponse)
        given(oauthClientPort.refresh()).willReturn(refreshedResponse)

        // when
        val result = OIDCTokenPublicKeyAdapter.findByKeyId(kid)

        // then
        assertThat(result.kid).isEqualTo(kid)
        then(oauthClientPort).should().fetch()
        then(oauthClientPort).should().refresh()
    }

    @Test
    @DisplayName("캐시에서 공개키 목록을 가져오지 못하면 InternalServerException을 발생시킨다")
    fun findByKeyId_fetchFailed_throwsException() {
        // given
        val kid = "any-kid"
        given(oauthClientPort.fetch()).willReturn(null)

        // when & then
        assertThrows<InternalServerException> {
            OIDCTokenPublicKeyAdapter.findByKeyId(kid)
        }.also {
            assertThat(it.message).contains("Redis로부터 공개키를 가져오는데 실패했습니다.")
        }
    }

    @Test
    @DisplayName("갱신 후에도 일치하는 kid의 공개키가 목록에 없으면 InternalServerException을 발생시킨다")
    fun findByKeyId_notFound_throws_Exception() {
        // given
        val kid = "non-existent-kid"
        val initialResponse = OIDCPublicKeyResponse(keys = listOf(OIDCPublicKey(kid = "other-kid")))
        val refreshedResponse = OIDCPublicKeyResponse(keys = listOf(OIDCPublicKey(kid = "still-other-kid")))

        `when`(oauthClientPort.fetch()).thenReturn(initialResponse)
        `when`(oauthClientPort.refresh()).thenReturn(refreshedResponse)

        // when & then
        assertThrows<InternalServerException> {
            OIDCTokenPublicKeyAdapter.findByKeyId(kid)
        }.also {
            assertThat(it.message).contains("공개키 목록에서 일치하는 키를 찾을 수 없습니다.")
        }
    }
}
