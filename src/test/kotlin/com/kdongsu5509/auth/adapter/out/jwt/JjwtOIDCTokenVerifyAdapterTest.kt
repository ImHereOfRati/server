package com.kdongsu5509.auth.adapter.out.jwt

import com.common.testsupport.TestJwtBuilder
import com.common.testsupport.jwt.OidcTestJwtProvider
import com.kdongsu5509.auth.application.service.dto.OIDCDecodePayload
import com.kdongsu5509.support.exception.type.UnauthorizedException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
class JjwtOIDCTokenVerifyAdapterTest {

    private lateinit var jjwtOIDCTokenVerifyAdapter: JjwtOIDCTokenVerifyAdapter

    @BeforeEach
    fun setUp() {
        jjwtOIDCTokenVerifyAdapter = JjwtOIDCTokenVerifyAdapter()
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
        val result = jjwtOIDCTokenVerifyAdapter.getKid(token)

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
            jjwtOIDCTokenVerifyAdapter.getKid(token)
        }.also {
            assertThat(it.message).contains("OIDC ID 토큰의 형식이나 구성이 올바르지 않습니다.")
        }
    }

    @Test
    @DisplayName("잘못된 토큰 형식인 경우 UnauthorizedException을 발생시킨다")
    fun getKid_invalidFormat_throwsException() {
        // given
        val token = "invalidTokenWithoutDots"

        // when & then
        assertThrows<UnauthorizedException> {
            jjwtOIDCTokenVerifyAdapter.getKid(token)
        }.also {
            assertThat(it.message).contains("OIDC ID 토큰의 형식이나 구성이 올바르지 않습니다.")
        }
    }

    @Test
    @DisplayName("페이로드의 issuer와 audience가 설정과 일치하면 검증을 통과한다")
    fun verifyPayLoad_success() {
        // given
        val issuer = "https://accounts.google.com"
        val audience = "test-app-key"
        val nonce = "test-nonce"
        val payload =
            OIDCDecodePayload(iss = "accounts.google.com", aud = audience, sub = "sub", nonce = nonce, email = "test@test.com", nickname = "nick")

        // when & then (예외가 발생하지 않아야 함)
        jjwtOIDCTokenVerifyAdapter.verifyPayLoad(payload, issuer, audience, nonce)
    }

    @Test
    @DisplayName("페이로드의 issuer가 일치하지 않으면 UnauthorizedException 발생시킨다")
    fun verifyPayLoad_invalidIssuer_throwsException() {
        // given
        val payload = OIDCDecodePayload(
            iss = "invalid-issuer",
            aud = "test-app-key",
            sub = "sub",
            nonce = "test-nonce",
            email = "test@test.com",
            nickname = "nick"
        )

        // when & then
        assertThrows<UnauthorizedException> {
            jjwtOIDCTokenVerifyAdapter.verifyPayLoad(payload, "https://kauth.kakao.com", "test-app-key", "test-nonce")
        }.also {
            assertThat(it.message).contains("OIDC ID 토큰의 형식이나 구성이 올바르지 않습니다.")
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
            nonce = "test-nonce",
            email = "test@test.com",
            nickname = "nick"
        )

        // when & then
        assertThrows<UnauthorizedException> {
            jjwtOIDCTokenVerifyAdapter.verifyPayLoad(payload, issuer, "valid-aud", "test-nonce")
        }.also {
            assertThat(it.message).contains("OIDC ID 토큰의 형식이나 구성이 올바르지 않습니다.")
        }
    }

    @Test
    @DisplayName("페이로드의 nonce가 일치하지 않으면 UnauthorizedException을 발생시킨다")
    fun verifyPayLoad_invalidNonce_throwsException() {
        // given
        val issuer = "https://kauth.kakao.com"
        val audience = "test-app-key"
        val payload = OIDCDecodePayload(
            iss = issuer,
            aud = audience,
            sub = "sub",
            nonce = "payload-nonce",
            email = "test@test.com",
            nickname = "nick"
        )

        // when & then
        assertThrows<UnauthorizedException> {
            jjwtOIDCTokenVerifyAdapter.verifyPayLoad(payload, issuer, audience, "expected-nonce")
        }.also {
            assertThat(it.message).contains("OIDC ID 토큰의 nonce 검증에 실패했습니다.")
        }
    }

    @Test
    @DisplayName("올바른 공개키 정보로 서명을 성공적으로 검증한다")
    fun verifySignature_success() {
        // given
        val token = TestJwtBuilder.buildDSKOIdToken()
        val publicKey = OidcTestJwtProvider.keyPair.public as java.security.interfaces.RSAPublicKey
        val modulus = Base64.getUrlEncoder().encodeToString(publicKey.modulus.toByteArray())
        val exponent = Base64.getUrlEncoder().encodeToString(publicKey.publicExponent.toByteArray())

        // when
        val result = jjwtOIDCTokenVerifyAdapter.verifySignature(token, modulus, exponent)

        // then
        assertThat(result).isNotNull
        assertThat(result.payload.issuer).isEqualTo(OidcTestJwtProvider.PAYLOAD_ISS)
    }

    @Test
    @DisplayName("서명이 일치하지 않거나 토큰 파싱 에러 시 UnauthorizedException 발생시킨다")
    fun verifySignature_invalidSignature_throwsException() {
        val token = "invalid.token.signature"
        val publicKey = OidcTestJwtProvider.keyPair.public as java.security.interfaces.RSAPublicKey
        val modulus = Base64.getUrlEncoder().encodeToString(publicKey.modulus.toByteArray())
        val exponent = Base64.getUrlEncoder().encodeToString(publicKey.publicExponent.toByteArray())

        assertThrows<UnauthorizedException> {
            jjwtOIDCTokenVerifyAdapter.verifySignature(token, modulus, exponent)
        }.also {
            assertThat(it.message).contains("OIDC ID 토큰의 서명 검증에 실패했습니다.")
        }
    }

    @Test
    @DisplayName("잘못된 인코딩 값이 입력되면 InvalidInputException 발생시킨다")
    fun createPublicKey_invalidEncoding_throwsException() {
        val token = "some.token.here"
        val invalidModulus = "invalid_base64!!"
        val exponent = "AQAB"

        assertThrows<com.kdongsu5509.support.exception.type.InvalidInputException> {
            jjwtOIDCTokenVerifyAdapter.verifySignature(token, invalidModulus, exponent)
        }.also {
            assertThat(it.message).contains("잘못된 Base64 인코딩 값입니다.")
        }
    }
}
