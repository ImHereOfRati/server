package com.kdongsu5509.user.adapter.out.auth.jwt

import com.common.testUtil.TestJwtBuilder
import com.kdongsu5509.support.exception.type.UnauthorizedException
import com.kdongsu5509.user.adapter.out.auth.oauth.KakaoOIDCProperties
import com.kdongsu5509.user.application.dto.OIDCDecodePayload
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
class JjwtOIDCIdTokenVerifyAdapterTest {

    @Mock
    private lateinit var kakaoOIDCProperties: KakaoOIDCProperties

    private lateinit var jjwtOIDCIdTokenVerifyAdapter: JjwtOIDCIdTokenVerifyAdapter

    @BeforeEach
    fun setUp() {
        jjwtOIDCIdTokenVerifyAdapter = JjwtOIDCIdTokenVerifyAdapter(kakaoOIDCProperties)
    }

    @Test
    @DisplayName("ID 토큰 헤더에서 kid를 성공적으로 추출한다")
    fun getKid_success() {
        // given
        val kid = "test-kid-123"
        val header = "{\"kid\":\"$kid\",\"alg\":\"RS256\"}"
        val encodedHeader = Base64.getUrlEncoder().encodeToString(header.toByteArray())
        val token = "$encodedHeader.payload.signature"

        // when
        val result = jjwtOIDCIdTokenVerifyAdapter.getKid(token)

        // then
        assertThat(result).isEqualTo(kid)
    }

    @Test
    @DisplayName("ID 토큰 헤더에 kid가 없으면 UnauthorizedException 발생시킨다")
    fun getKid_noKid_throwsException() {
        // given
        val header = "{\"alg\":\"RS256\"}"
        val encodedHeader = Base64.getUrlEncoder().encodeToString(header.toByteArray())
        val token = "$encodedHeader.payload.signature"

        // when & then
        assertThrows<UnauthorizedException> {
            jjwtOIDCIdTokenVerifyAdapter.getKid(token)
        }.also {
            assertThat(it.message).contains("ID 토큰 헤더에 kid가 없습니다.")
        }
    }

    @Test
    @DisplayName("잘못된 토큰 형식인 경우 UnauthorizedException을 발생시킨다")
    fun getKid_invalidFormat_throwsException() {
        // given
        val token = "invalidTokenWithoutDots"

        // when & then
        assertThrows<UnauthorizedException> {
            jjwtOIDCIdTokenVerifyAdapter.getKid(token)
        }.also {
            assertThat(it.message).contains("잘못된 토큰 형식입니다.")
        }
    }

    @Test
    @DisplayName("페이로드의 issuer와 audience가 설정과 일치하면 검증을 통과한다")
    fun verifyPayLoad_success() {
        // given
        val issuer = "https://kauth.kakao.com"
        val audience = "test-app-key"
        val payload =
            OIDCDecodePayload(iss = issuer, aud = audience, sub = "sub", email = "test@test.com", nickname = "nick")

        `when`(kakaoOIDCProperties.issuer).thenReturn(issuer)
        `when`(kakaoOIDCProperties.audience).thenReturn(audience)

        // when & then (예외가 발생하지 않아야 함)
        jjwtOIDCIdTokenVerifyAdapter.verifyPayLoad(payload)
    }

    @Test
    @DisplayName("페이로드의 issuer가 일치하지 않으면 UnauthorizedException 발생시킨다")
    fun verifyPayLoad_invalidIssuer_throwsException() {
        // given
        val payload = OIDCDecodePayload(
            iss = "invalid-issuer",
            aud = "test-app-key",
            sub = "sub",
            email = "test@test.com",
            nickname = "nick"
        )

        `when`(kakaoOIDCProperties.issuer).thenReturn("https://kauth.kakao.com")

        // when & then
        assertThrows<UnauthorizedException> {
            jjwtOIDCIdTokenVerifyAdapter.verifyPayLoad(payload)
        }.also {
            assertThat(it.message).contains("토큰의 issuer가 일치하지 않습니다.")
        }
    }

    @Test
    @DisplayName("페이로드의 audience가 일치하지 않으면 UnauthorizedException을 발생시킨다")
    fun verifyPayLoad_invalidAudience_throwsException() {
        // given
        val issuer = "https://kauth.kakao.com"
        val payload = OIDCDecodePayload(
            iss = issuer,
            aud = "invalid-aud",
            sub = "sub",
            email = "test@test.com",
            nickname = "nick"
        )

        `when`(kakaoOIDCProperties.issuer).thenReturn(issuer)
        `when`(kakaoOIDCProperties.audience).thenReturn("valid-aud")

        // when & then
        assertThrows<UnauthorizedException> {
            jjwtOIDCIdTokenVerifyAdapter.verifyPayLoad(payload)
        }.also {
            assertThat(it.message).contains("토큰의 audience가 일치하지 않습니다.")
        }
    }

    @Test
    @DisplayName("올바른 공개키 정보로 서명을 성공적으로 검증한다")
    fun verifySignature_success() {
        // given
        val token = TestJwtBuilder.buildValidIdToken()
        val publicKey = TestJwtBuilder.keyPair.public as java.security.interfaces.RSAPublicKey
        val modulus = Base64.getUrlEncoder().encodeToString(publicKey.modulus.toByteArray())
        val exponent = Base64.getUrlEncoder().encodeToString(publicKey.publicExponent.toByteArray())

        // when
        val result = jjwtOIDCIdTokenVerifyAdapter.verifySignature(token, modulus, exponent)

        // then
        assertThat(result).isNotNull
        assertThat(result.payload.issuer).isEqualTo(TestJwtBuilder.KAKAO_PAYLOAD_ISS)
    }
}
