package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.adapter.out.oauth.OIDCProperties
import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKey
import com.kdongsu5509.auth.application.port.out.OIDCIdTokenVerifyPort
import com.kdongsu5509.auth.application.port.out.PublicKeyLoadPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.support.exception.type.UnauthorizedException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

@ExtendWith(MockitoExtension::class)
class OIDCVerifyServiceTest {

    companion object {
        private const val ID_TOKEN = "test-id-token"
        private const val NONCE = "test-nonce"
        private const val KID = "test-kid"
        private const val MODULUS = "n-value"
        private const val EXPONENT = "e-value"
        private const val EMAIL = "test@kakao.com"
        private const val NICKNAME = "카카오친구"
        private const val SUB = "kakao-sub"

        private val PUBLIC_KEY = OIDCPublicKey(kid = KID, n = MODULUS, e = EXPONENT)
    }

    @Mock
    private lateinit var oidcIdTokenVerifyPort: OIDCIdTokenVerifyPort

    @Mock
    private lateinit var publicKeyLoadPort: PublicKeyLoadPort

    private lateinit var oidcProperties: OIDCProperties

    private lateinit var verifyService: OIDCVerifyService

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
        verifyService = OIDCVerifyService(oidcIdTokenVerifyPort, publicKeyLoadPort, oidcProperties)
    }

    @Test
    @DisplayName("Google ID 토큰 검증에 성공하여 유저 정보를 반환한다")
    fun verify_success_google() {
        // given
        givenTokenVerificationSucceeds(OAuth2Provider.GOOGLE, "accounts.google.com", "google-client-id", NONCE, null, "구글친구")

        // when
        val result = verifyService.verify(OAuth2Provider.GOOGLE, ID_TOKEN, NONCE)

        // then
        assertThat(result.email).isEqualTo(EMAIL)
        assertThat(result.nickname).isEqualTo("구글친구")
        assertThat(result.sub).isEqualTo(SUB)

        then(oidcIdTokenVerifyPort).should().verifyPayLoad(any(), eq("https://accounts.google.com"), eq("google-client-id"), eq(NONCE))
    }

    @Test
    @DisplayName("nonce가 없으면 예외가 발생한다")
    fun verify_fail_missing_nonce() {
        // given
        givenTokenSignatureVerificationSucceeds(OAuth2Provider.KAKAO, "https://kauth.kakao.com", "kakao-client-id")
        givenClaimsHasEmail(EMAIL, NONCE)

        // when & then
        assertUnauthorizedException("OIDC ID 토큰의 nonce 검증에 실패했습니다.") {
            verifyService.verify(OAuth2Provider.KAKAO, ID_TOKEN, "")
        }
    }

    @Test
    @DisplayName("ID 토큰에 이메일 정보가 없으면 예외가 발생한다")
    fun verify_fail_missing_email() {
        // given
        givenTokenSignatureVerificationSucceeds(OAuth2Provider.KAKAO, "https://kauth.kakao.com", "kakao-client-id")
        givenClaimsHasEmail(null, NONCE)

        // when & then
        assertUnauthorizedException("ID 토큰에 이메일 정보가 없습니다.") {
            verifyService.verify(OAuth2Provider.KAKAO, ID_TOKEN, NONCE)
        }
    }

    @Test
    @DisplayName("공개키를 찾을 수 없으면 예외가 발생한다")
    fun verify_fail_public_key_not_found() {

        // given
        given(oidcIdTokenVerifyPort.getKid(ID_TOKEN)).willReturn(KID)
        given(publicKeyLoadPort.findByKeyId(OAuth2Provider.KAKAO, KID)).willThrow(UnauthorizedException(AuthException.IMHERE_KEY_NOT_FOUND_IN_REDIS.errorMessage))

        // when & then
        assertUnauthorizedException(AuthException.IMHERE_KEY_NOT_FOUND_IN_REDIS.errorMessage) {
            verifyService.verify(OAuth2Provider.KAKAO, ID_TOKEN, NONCE)
        }
    }

    private fun givenTokenVerificationSucceeds(
        provider: OAuth2Provider,
        issuer: String,
        audience: String,
        nonce: String,
        nickname: String?,
        name: String?
    ) {
        givenTokenSignatureVerificationSucceeds(provider, issuer, audience)
        val mockClaims = givenClaimsHasEmail(EMAIL, nonce)
        given(mockClaims["nickname"]).willReturn(nickname)
        given(mockClaims["name"]).willReturn(name)
        given(mockClaims.issuer).willReturn(issuer)
        given(mockClaims.audience).willReturn(setOf(audience))
        given(mockClaims.subject).willReturn(SUB)
    }

    private fun givenTokenSignatureVerificationSucceeds(
        provider: OAuth2Provider,
        issuer: String,
        audience: String
    ) {
        @Suppress("UNCHECKED_CAST")
        val mockJws = mock(Jws::class.java) as Jws<Claims>
        given(oidcIdTokenVerifyPort.getKid(ID_TOKEN)).willReturn(KID)
        given(publicKeyLoadPort.findByKeyId(provider, KID)).willReturn(PUBLIC_KEY)
        given(oidcIdTokenVerifyPort.verifySignature(ID_TOKEN, MODULUS, EXPONENT)).willReturn(mockJws)
    }

    private fun givenClaimsHasEmail(email: String?, nonce: String): Claims {
        val mockJws = oidcIdTokenVerifyPort.verifySignature(ID_TOKEN, MODULUS, EXPONENT)
        val mockClaims = mock(Claims::class.java)
        given(mockJws.payload).willReturn(mockClaims)
        given(mockClaims["email"]).willReturn(email)
        given(mockClaims["nonce"]).willReturn(nonce)
        return mockClaims
    }

    private fun assertUnauthorizedException(message: String, block: () -> Unit) {
        val exception = assertThrows<UnauthorizedException>(block)
        assertThat(exception.message).contains(message)
    }
}
