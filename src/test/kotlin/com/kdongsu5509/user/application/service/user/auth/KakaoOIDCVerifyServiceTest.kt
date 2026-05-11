package com.kdongsu5509.user.application.service.user.auth

import com.kdongsu5509.support.exception.type.UnauthorizedException
import com.kdongsu5509.user.adapter.out.auth.oauth.dto.OIDCPublicKey
import com.kdongsu5509.user.application.port.out.user.oauth.OIDCIdTokenVerifyPort
import com.kdongsu5509.user.application.port.out.user.oauth.PublicKeyLoadPort
import com.kdongsu5509.user.exception.AuthError
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

@ExtendWith(MockitoExtension::class)
class KakaoOIDCVerifyServiceTest {

    companion object {
        private const val ID_TOKEN = "test-id-token"
        private const val KID = "test-kid"
        private const val MODULUS = "n-value"
        private const val EXPONENT = "e-value"
        private const val EMAIL = "test@kakao.com"
        private const val NICKNAME = "카카오친구"

        private val PUBLIC_KEY = OIDCPublicKey(kid = KID, n = MODULUS, e = EXPONENT)
    }

    @Mock
    private lateinit var oidcIdTokenVerifyPort: OIDCIdTokenVerifyPort

    @Mock
    private lateinit var publicKeyLoadPort: PublicKeyLoadPort

    private lateinit var verifyService: KakaoOIDCVerifyService

    @BeforeEach
    fun setUp() {
        verifyService = KakaoOIDCVerifyService(oidcIdTokenVerifyPort, publicKeyLoadPort)
    }

    @Test
    @DisplayName("카카오 ID 토큰 검증에 성공하여 유저 정보를 반환한다")
    fun verify_success() {
        // given
        givenTokenVerificationSucceeds()

        // when
        val result = verifyService.verify(ID_TOKEN)

        // then
        assertThat(result.email).isEqualTo(EMAIL)
        assertThat(result.nickname).isEqualTo(NICKNAME)

        then(oidcIdTokenVerifyPort).should().verifyPayLoad(any())
    }

    @Test
    @DisplayName("ID 토큰에 이메일 정보가 없으면 예외가 발생한다")
    fun verify_fail_missing_email() {
        // given
        givenTokenSignatureVerificationSucceeds()
        givenClaimsHasEmail(null)

        // when & then
        assertUnauthorizedException("ID 토큰에 이메일 정보가 없습니다.") {
            verifyService.verify(ID_TOKEN)
        }
    }

    @Test
    @DisplayName("공개키를 찾을 수 없으면 예외가 발생한다")
    fun verify_fail_public_key_not_found() {
        // given
        given(oidcIdTokenVerifyPort.getKid(ID_TOKEN)).willReturn(KID)
        given(publicKeyLoadPort.loadPublicKey(KID)).willThrow(UnauthorizedException(AuthError.IMHERE_KEY_NOT_FOUND_IN_REDIS.message!!))

        // when & then
        assertUnauthorizedException(AuthError.IMHERE_KEY_NOT_FOUND_IN_REDIS.message!!) {
            verifyService.verify(ID_TOKEN)
        }
    }

    private fun givenTokenVerificationSucceeds() {
        givenTokenSignatureVerificationSucceeds()
        val mockClaims = givenClaimsHasEmail(EMAIL)
        given(mockClaims["nickname"]).willReturn(NICKNAME)
        given(mockClaims.issuer).willReturn("https://kauth.kakao.com")
        given(mockClaims.audience).willReturn(setOf("test-client-id"))
        given(mockClaims.subject).willReturn("test-sub")
    }

    private fun givenTokenSignatureVerificationSucceeds() {
        val mockJws = mock(Jws::class.java) as Jws<Claims>
        given(oidcIdTokenVerifyPort.getKid(ID_TOKEN)).willReturn(KID)
        given(publicKeyLoadPort.loadPublicKey(KID)).willReturn(PUBLIC_KEY)
        given(oidcIdTokenVerifyPort.verifySignature(ID_TOKEN, MODULUS, EXPONENT)).willReturn(mockJws)
    }

    private fun givenClaimsHasEmail(email: String?): Claims {
        val mockJws = oidcIdTokenVerifyPort.verifySignature(ID_TOKEN, MODULUS, EXPONENT)
        val mockClaims = mock(Claims::class.java)
        given(mockJws.payload).willReturn(mockClaims)
        given(mockClaims["email"]).willReturn(email)
        return mockClaims
    }

    private fun assertUnauthorizedException(message: String, block: () -> Unit) {
        val exception = assertThrows<UnauthorizedException>(block)
        assertThat(exception.message).contains(message)
    }
}
