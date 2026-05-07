package com.kdongsu5509.user.adapter.out.auth

import com.kdongsu5509.support.exception.AuthErrorCode
import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.user.adapter.out.auth.oauth.KakaoOIDCProperties
import com.kdongsu5509.user.adapter.out.auth.oauth.dto.OIDCPublicKey
import com.kdongsu5509.user.application.port.out.user.JwtVerificationPort
import com.kdongsu5509.user.application.port.out.user.oauth.PublicKeyLoadPort
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
class JjwtParserAdapterTest {

    @Mock
    private lateinit var publicKeyLoadPort: PublicKeyLoadPort

    @Mock
    private lateinit var jwtVerificationPort: JwtVerificationPort

    private lateinit var kakaoOIDCProperties: KakaoOIDCProperties
    private lateinit var jjwtParserAdapter: JjwtParserAdapter

    @BeforeEach
    fun setUp() {
        kakaoOIDCProperties = KakaoOIDCProperties(
            issuer = "https://kauth.kakao.com",
            audience = "test-audience",
            cacheKey = "kakao:oidc:public-keys"
        )
        jjwtParserAdapter = JjwtParserAdapter(
            publicKeyLoadPort,
            kakaoOIDCProperties,
            jwtVerificationPort
        )
    }

    @Test
    @DisplayName("유효한 ID 토큰을 파싱하여 OIDCDecodePayload를 반환한다")
    fun parse_validToken_success() {
        // given
        val kid = "test-kid-123"
        val email = "test@example.com"
        val sub = "test-sub-123"

        val unsignedToken = createUnsignedToken(kid)
        val signedToken = createSignedToken(unsignedToken, email, sub)

        val oidcPublicKey = OIDCPublicKey(
            kid = kid,
            kty = "RSA",
            alg = "RS256",
            use = "sig",
            n = "test-modulus",
            e = "AQAB"
        )

        val jws = createMockJws(email, sub)

        `when`(publicKeyLoadPort.loadPublicKey(kid)).thenReturn(oidcPublicKey)
        `when`(jwtVerificationPort.verifySignature(signedToken, oidcPublicKey.n, oidcPublicKey.e))
            .thenReturn(jws)

        // when
        val result = jjwtParserAdapter.parse(signedToken)

        // then
        assertThat(result).isNotNull()
        assertThat(result.iss).isEqualTo("https://kauth.kakao.com")
        assertThat(result.aud).isEqualTo("test-audience")
        assertThat(result.sub).isEqualTo(sub)
        assertThat(result.email).isEqualTo(email)

        verify(publicKeyLoadPort).loadPublicKey(kid)
        verify(jwtVerificationPort).verifySignature(signedToken, oidcPublicKey.n, oidcPublicKey.e)
    }

    @Test
    @DisplayName("토큰 헤더에서 kid를 성공적으로 추출한다")
    fun getKidFromOriginTokenHeader_success() {
        // given
        val kid = "test-kid-456"
        val unsignedToken = createUnsignedToken(kid)
        val signedToken = createSignedToken(unsignedToken, "test@example.com", "test-sub")

        // when
        val result = jjwtParserAdapter.getKidFromOriginTokenHeader(signedToken)

        // then
        assertThat(result).isEqualTo(kid)
    }

    @Test
    @DisplayName("잘못된 형식의 토큰은 OIDCInvalidException을 발생시킨다")
    fun getKidFromOriginTokenHeader_invalidFormat_throwsException() {
        // given
        val invalidToken = "invalid.token" // 2개 부분만 있음

        // when & then
        assertThrows<BusinessException> {
            jjwtParserAdapter.getKidFromOriginTokenHeader(invalidToken)
        }.also { exception ->
            assertThat(exception.message).contains(AuthErrorCode.OIDC_INVALID.message)
        }
    }

    @Test
    @DisplayName("빈 토큰은 OIDCInvalidException을 발생시킨다")
    fun getKidFromOriginTokenHeader_emptyToken_throwsException() {
        // given
        val emptyToken = ""

        // when & then
        assertThrows<BusinessException> {
            jjwtParserAdapter.getKidFromOriginTokenHeader(emptyToken)
        }
    }

    @Test
    @DisplayName("JWS에서 페이로드를 성공적으로 추출한다")
    fun extractPayloadFromJws_success() {
        // given
        val email = "test@example.com"
        val sub = "test-sub-999"
        val jws = createMockJws(email, sub)

        // when - extractPayloadFromJws는 private이므로 parse를 통해 간접 테스트
        val kid = "test-kid-extract"
        val unsignedToken = createUnsignedToken(kid)
        val signedToken = createSignedToken(unsignedToken, email, sub)

        val oidcPublicKey = OIDCPublicKey(
            kid = kid,
            kty = "RSA",
            alg = "RS256",
            use = "sig",
            n = "test-modulus",
            e = "AQAB"
        )

        `when`(publicKeyLoadPort.loadPublicKey(kid)).thenReturn(oidcPublicKey)
        `when`(jwtVerificationPort.verifySignature(signedToken, oidcPublicKey.n, oidcPublicKey.e))
            .thenReturn(jws)

        // when
        val result = jjwtParserAdapter.parse(signedToken)

        // then
        assertThat(result.email).isEqualTo(email)
        assertThat(result.sub).isEqualTo(sub)
        assertThat(result.iss).isEqualTo("https://kauth.kakao.com")
        assertThat(result.aud).isEqualTo("test-audience")
    }

    @Test
    @DisplayName("이메일이 없는 페이로드도 처리할 수 있다")
    fun parse_tokenWithoutEmail_success() {
        // given
        val kid = "test-kid-no-email"
        val unsignedToken = createUnsignedToken(kid)
        val signedToken = createSignedTokenWithoutEmail(unsignedToken, "test-sub")

        val oidcPublicKey = OIDCPublicKey(
            kid = kid,
            kty = "RSA",
            alg = "RS256",
            use = "sig",
            n = "test-modulus",
            e = "AQAB"
        )

        val jws = createMockJwsWithoutEmail("test-sub")

        `when`(publicKeyLoadPort.loadPublicKey(kid)).thenReturn(oidcPublicKey)
        `when`(jwtVerificationPort.verifySignature(signedToken, oidcPublicKey.n, oidcPublicKey.e))
            .thenReturn(jws)

        // when
        val result = jjwtParserAdapter.parse(signedToken)

        // then
        assertThat(result).isNotNull()
        assertThat(result.email).isNull()
        assertThat(result.sub).isEqualTo("test-sub")
    }

    private fun createUnsignedToken(kid: String): String {
        val header = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"kid":"$kid","alg":"RS256"}""".toByteArray()
        )
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"iss":"https://kauth.kakao.com","aud":"test-audience","sub":"test-sub"}""".toByteArray()
        )
        return "$header.$payload."
    }

    private fun createSignedToken(unsignedToken: String, email: String, sub: String): String {
        val parts = unsignedToken.split(".")
        val header = parts[0]
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"iss":"https://kauth.kakao.com","aud":"test-audience","sub":"$sub","email":"$email"}""".toByteArray()
        )
        val signature = "test-signature"
        return "$header.$payload.$signature"
    }

    private fun createSignedTokenWithoutEmail(unsignedToken: String, sub: String): String {
        val parts = unsignedToken.split(".")
        val header = parts[0]
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
            """{"iss":"https://kauth.kakao.com","aud":"test-audience","sub":"$sub"}""".toByteArray()
        )
        val signature = "test-signature"
        return "$header.$payload.$signature"
    }

    private fun createMockJws(email: String, sub: String): Jws<Claims> {
        val claims = Jwts.claims()
            .issuer("https://kauth.kakao.com")
            .audience().add("test-audience").and()
            .subject(sub)
            .add("email", email)
            .build()

        @Suppress("UNCHECKED_CAST")
        val jws: Jws<Claims> = mock(Jws::class.java) as Jws<Claims>
        `when`(jws.payload).thenReturn(claims)
        return jws
    }

    private fun createMockJwsWithoutEmail(sub: String): Jws<Claims> {
        val claims = Jwts.claims()
            .issuer("https://kauth.kakao.com")
            .audience().add("test-audience").and()
            .subject(sub)
            .build()

        @Suppress("UNCHECKED_CAST")
        val jws: Jws<Claims> = mock(Jws::class.java) as Jws<Claims>
        `when`(jws.payload).thenReturn(claims)
        return jws
    }
}
