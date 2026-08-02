package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.adapter.out.oauth.OIDCProperties
import com.kdongsu5509.auth.adapter.out.oauth.dto.OIDCPublicKey
import com.kdongsu5509.auth.application.port.out.OIDCIdTokenVerifyPort
import com.kdongsu5509.auth.application.port.out.PublicKeyLoadPort
import com.kdongsu5509.user.domain.OAuth2Provider
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
                    issuers = mutableListOf("https://kauth.kakao.com"),
                    audiences = mutableListOf("kakao-client-id"),
                    cacheKey = "kakao-cache",
                    jwksUri = "https://kauth.kakao.com/.well-known/jwks.json"
                ),
                "google" to OIDCProperties.Provider(
                    issuers = mutableListOf("https://accounts.google.com", "accounts.google.com"),
                    audiences = mutableListOf("google-web-client-id", "google-ios-client-id"),
                    cacheKey = "google-cache",
                    jwksUri = "https://www.googleapis.com/oauth2/v3/certs"
                ),
                "apple" to OIDCProperties.Provider(
                    issuers = mutableListOf("https://appleid.apple.com"),
                    audiences = mutableListOf("apple-bundle-id"),
                    cacheKey = "apple-cache",
                    jwksUri = "https://appleid.apple.com/auth/keys"
                )
            )
        )
        verifyService = OIDCVerifyService(oidcIdTokenVerifyPort, publicKeyLoadPort, oidcProperties)
    }

    @Test
    @DisplayName("Google ID 토큰 검증에 성공하여 유저 정보를 반환한다")
    fun verify_success_google() {
        // given: Google ID 토큰에는 nickname이 없고 name만 있다.
        givenTokenVerificationSucceeds(
            OAuth2Provider.GOOGLE,
            "accounts.google.com",
            "google-ios-client-id",
            NONCE,
            null,
            "구글친구"
        )

        // when
        val result = verifyService.verify(OAuth2Provider.GOOGLE, ID_TOKEN, NONCE)

        // then
        assertThat(result.email).isEqualTo(EMAIL)
        assertThat(result.nickname).isEqualTo("구글친구")
        assertThat(result.sub).isEqualTo(SUB)

        // 허용 issuer·audience는 설정에 담긴 목록이 그대로 넘어간다.
        then(oidcIdTokenVerifyPort).should().verifyPayLoad(
            any(),
            eq(listOf("https://accounts.google.com", "accounts.google.com")),
            eq(listOf("google-web-client-id", "google-ios-client-id")),
            eq(NONCE)
        )
    }

    @Test
    @DisplayName("Apple ID 토큰은 표시 이름이 없어 이메일 앞부분을 닉네임으로 쓴다")
    fun verify_success_apple() {
        // given: Apple ID 토큰에는 nickname도 name도 없다. 표시 이름은 인가 응답 본문으로만 온다.
        givenTokenVerificationSucceeds(
            OAuth2Provider.APPLE,
            "https://appleid.apple.com",
            "apple-bundle-id",
            NONCE,
            null,
            null
        )

        // when
        val result = verifyService.verify(OAuth2Provider.APPLE, ID_TOKEN, NONCE)

        // then
        assertThat(result.email).isEqualTo(EMAIL)
        assertThat(result.nickname).isEqualTo(EMAIL.substringBefore("@"))
        assertThat(result.sub).isEqualTo(SUB)

        then(oidcIdTokenVerifyPort).should().verifyPayLoad(
            any(),
            eq(listOf("https://appleid.apple.com")),
            eq(listOf("apple-bundle-id")),
            eq(NONCE)
        )
    }

    @Test
    @DisplayName("Apple ID 토큰에 이메일이 없으면 로그인을 거절한다")
    fun verify_fail_apple_missing_email() {
        // given: 사용자가 email scope를 주지 않은 경우다. users.email이 필수라 가입시킬 수 없다.
        givenTokenSignatureVerificationSucceeds(
            OAuth2Provider.APPLE,
            "https://appleid.apple.com",
            "apple-bundle-id"
        )
        givenClaimsHasEmail(null, NONCE)

        // when & then
        assertUnauthorizedException("ID 토큰에 이메일 정보가 없습니다.") {
            verifyService.verify(OAuth2Provider.APPLE, ID_TOKEN, NONCE)
        }
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
        given(
            publicKeyLoadPort.findByKeyId(
                OAuth2Provider.KAKAO,
                KID
            )
        ).willThrow(UnauthorizedException(AuthException.IMHERE_KEY_NOT_FOUND_IN_CACHE.errorMessage))

        // when & then
        assertUnauthorizedException(AuthException.IMHERE_KEY_NOT_FOUND_IN_CACHE.errorMessage) {
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
