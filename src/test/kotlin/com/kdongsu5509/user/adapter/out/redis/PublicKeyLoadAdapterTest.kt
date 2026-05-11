package com.kdongsu5509.user.adapter.out.redis

import com.kdongsu5509.support.exception.type.InfraFailureException
import com.kdongsu5509.user.adapter.out.auth.oauth.dto.OIDCPublicKey
import com.kdongsu5509.user.adapter.out.auth.oauth.dto.OIDCPublicKeyResponse
import com.kdongsu5509.user.application.port.out.user.oauth.OauthClientPort
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class PublicKeyLoadAdapterTest {

    @Mock
    private lateinit var oauthClientPort: OauthClientPort

    private lateinit var publicKeyLoadAdapter: PublicKeyLoadAdapter

    @BeforeEach
    fun setUp() {
        publicKeyLoadAdapter = PublicKeyLoadAdapter(oauthClientPort)
    }

    @Test
    @DisplayName("kid가 일치하는 공개키가 캐시에 있으면 성공적으로 반환한다")
    fun loadPublicKey_success() {
        // given
        val kid = "test-kid"
        val expectedKey = OIDCPublicKey(kid = kid, n = "modulus", e = "exponent")
        val response = OIDCPublicKeyResponse(keys = listOf(expectedKey))

        `when`(oauthClientPort.fetchPublicKey()).thenReturn(response)

        // when
        val result = publicKeyLoadAdapter.loadPublicKey(kid)

        // then
        assertThat(result.kid).isEqualTo(kid)
        assertThat(result.n).isEqualTo("modulus")
        assertThat(result.e).isEqualTo("exponent")
    }

    @Test
    @DisplayName("캐시에서 공개키 목록을 가져오지 못하면 InfraFailureException을 발생시킨다")
    fun loadPublicKey_fetchFailed_throwsException() {
        // given
        val kid = "any-kid"
        `when`(oauthClientPort.fetchPublicKey()).thenReturn(null)

        // when & then
        assertThrows<InfraFailureException> {
            publicKeyLoadAdapter.loadPublicKey(kid)
        }.also {
            assertThat(it.message).contains("Redis로부터 공개키를 가져오는데 실패했습니다.")
        }
    }

    @Test
    @DisplayName("일치하는 kid의 공개키가 목록에 없으면 InfraFailureException을 발생시킨다")
    fun loadPublicKey_notFound_throwsException() {
        // given
        val kid = "non-existent-kid"
        val response = OIDCPublicKeyResponse(keys = listOf(OIDCPublicKey(kid = "other-kid")))

        `when`(oauthClientPort.fetchPublicKey()).thenReturn(response)

        // when & then
        assertThrows<InfraFailureException> {
            publicKeyLoadAdapter.loadPublicKey(kid)
        }.also {
            assertThat(it.message).contains("공개키 목록에서 일치하는 키를 찾을 수 없습니다.")
        }
    }
}
