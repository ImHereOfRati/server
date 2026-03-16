package com.kdongsu5509.user.adapter.out.auth

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.user.adapter.out.auth.oauth.KakaoOIDCProperties
import com.kdongsu5509.user.application.dto.OIDCDecodePayload
import io.jsonwebtoken.Jwts
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.util.*

@DisplayName("JjwtVerifyAdapter 테스트")
class JjwtVerifyAdapterTest {

    private lateinit var kakaoOIDCProperties: KakaoOIDCProperties
    private lateinit var jjwtVerifyAdapter: JjwtVerifyAdapter
    private lateinit var keyPair: KeyPair

    private val payload = OIDCDecodePayload(
        iss = "https://kauth.kakao.com",
        aud = "test-audience",
        sub = "test-sub",
        email = "test@example.com",
        nickname = "고동수"
    )

    @BeforeEach
    fun setUp() {
        kakaoOIDCProperties = KakaoOIDCProperties(
            issuer = "https://kauth.kakao.com",
            audience = "test-audience",
            cacheKey = "kakao:oidc:public-keys"
        )
        jjwtVerifyAdapter = JjwtVerifyAdapter(kakaoOIDCProperties)

        // 테스트용 RSA 키 쌍 생성
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        keyPair = keyPairGenerator.generateKeyPair()
    }

    @Test
    @DisplayName("유효한 페이로드를 검증한다")
    fun verifyPayLoad_validPayload_success() {
        // when & then - 예외가 발생하지 않아야 함
        jjwtVerifyAdapter.verifyPayLoad(payload)
    }

    @Test
    @DisplayName("issuer가 일치하지 않으면 OIDCInvalidException을 발생시킨다")
    fun verifyPayLoad_invalidIssuer_throwsException() {
        val badPayload = OIDCDecodePayload(
            iss = "https://dsko.dsko.com",
            aud = "test-audience",
            sub = "test-sub",
            email = "test@example.com",
            nickname = "고동수"
        )
        // when & then
        assertThrows<BusinessException> {
            jjwtVerifyAdapter.verifyPayLoad(badPayload)
        }.also { exception ->
            assertThat(exception.message).contains("토큰의 issuer가 일치하지 않습니다")
            assertThat(exception.message).contains("https://dsko.dsko.com")
        }
    }

    @Test
    @DisplayName("audience가 일치하지 않으면 OIDCInvalidException을 발생시킨다")
    fun verifyPayLoad_invalidAudience_throwsException() {
        val badPayload = OIDCDecodePayload(
            iss = "https://kauth.kakao.com",
            aud = "bad-audience",
            sub = "test-sub",
            email = "test@example.com",
            nickname = "고동수"
        )
        // when & then
        assertThrows<BusinessException> {
            jjwtVerifyAdapter.verifyPayLoad(badPayload)
        }.also { exception ->
            assertThat(exception.message).contains("토큰의 audience가 일치하지 않습니다")
            assertThat(exception.message).contains("bad-audience")
        }
    }

    @Test
    @DisplayName("유효한 서명으로 토큰을 검증한다")
    fun verifySignature_validToken_success() {
        // given
        val token = createValidJwtToken()
        val rsaPublicKey = keyPair.public as RSAPublicKey
        val modulus = encodeBase64Url(rsaPublicKey.modulus)
        val exponent = encodeBase64Url(rsaPublicKey.publicExponent)

        // when
        val result = jjwtVerifyAdapter.verifySignature(token, modulus, exponent)

        // then
        assertThat(result).isNotNull()
        assertThat(result.body.subject).isEqualTo("test-sub")
    }

    @Test
    @DisplayName("만료된 토큰은 OIDCExpiredException을 발생시킨다")
    fun verifySignature_expiredToken_throwsException() {
        // given
        val expiredToken = createExpiredJwtToken()
        val rsaPublicKey = keyPair.public as RSAPublicKey
        val modulus = encodeBase64Url(rsaPublicKey.modulus)
        val exponent = encodeBase64Url(rsaPublicKey.publicExponent)

        // when & then
        assertThrows<BusinessException> {
            jjwtVerifyAdapter.verifySignature(expiredToken, modulus, exponent)
        }
    }

    @Test
    @DisplayName("잘못된 modulus 형식은 InvalidEncodingException을 발생시킨다")
    fun verifySignature_invalidModulusEncoding_throwsException() {
        // given
        val token = createValidJwtToken()
        val invalidModulus = "invalid-base64-url-encoding!!!"
        val rsaPublicKey = keyPair.public as RSAPublicKey
        val exponent = encodeBase64Url(rsaPublicKey.publicExponent)

        // when & then
        assertThrows<BusinessException> {
            jjwtVerifyAdapter.verifySignature(token, invalidModulus, exponent)
        }
    }

    @Test
    @DisplayName("잘못된 exponent 형식은 InvalidEncodingException을 발생시킨다")
    fun verifySignature_invalidExponentEncoding_throwsException() {
        // given
        val token = createValidJwtToken()
        val rsaPublicKey = keyPair.public as RSAPublicKey
        val modulus = encodeBase64Url(rsaPublicKey.modulus)
        val invalidExponent = "invalid-base64-url-encoding!!!"

        // when & then
        assertThrows<BusinessException> {
            jjwtVerifyAdapter.verifySignature(token, modulus, invalidExponent)
        }
    }

    @Test
    @DisplayName("RSA 키 생성 실패 시 적절한 예외를 발생시킨다")
    fun verifySignature_invalidKeySpec_throwsException() {
        // given
        val token = createValidJwtToken()
        // 잘못된 형식의 modulus와 exponent (너무 짧은 바이트 배열)
        val invalidModulus = Base64.getUrlEncoder().withoutPadding().encodeToString(byteArrayOf(1, 2, 3))
        val invalidExponent = Base64.getUrlEncoder().withoutPadding().encodeToString(byteArrayOf(1, 2, 3))

        // when & then
        assertThrows<BusinessException> {
            jjwtVerifyAdapter.verifySignature(token, invalidModulus, invalidExponent)
        }
    }

    private fun createValidJwtToken(): String {
        return Jwts.builder()
            .setSubject("test-sub")
            .setIssuer("https://kauth.kakao.com")
            .setAudience("test-audience")
            .setIssuedAt(Date())
            .setExpiration(Date(System.currentTimeMillis() + 3600000)) // 1시간 후
            .claim("email", "test@example.com")
            .signWith(keyPair.private)
            .compact()
    }

    private fun createExpiredJwtToken(): String {
        val pastDate = Date(System.currentTimeMillis() - 3600000) // 1시간 전
        return Jwts.builder()
            .setSubject("test-sub")
            .setIssuer("https://kauth.kakao.com")
            .setAudience("test-audience")
            .setIssuedAt(pastDate)
            .setExpiration(pastDate)
            .claim("email", "test@example.com")
            .signWith(keyPair.private)
            .compact()
    }

    private fun encodeBase64Url(bigInteger: BigInteger): String {
        val bytes = bigInteger.toByteArray()
        val positiveBytes = if (bytes[0] == 0.toByte()) bytes.sliceArray(1 until bytes.size) else bytes
        return Base64.getUrlEncoder().withoutPadding().encodeToString(positiveBytes)
    }
}

