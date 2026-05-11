//package com.kdongsu5509.user.adapter.out.auth
//
//import com.kdongsu5509.support.exception.BaseException
//import com.kdongsu5509.user.adapter.out.auth.jwt.JjwtVerifyAdapter
//import com.kdongsu5509.user.adapter.out.auth.oauth.KakaoOIDCProperties
//import com.kdongsu5509.user.application.dto.OIDCDecodePayload
//import io.jsonwebtoken.Jwts
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.DisplayName
//import org.junit.jupiter.api.Test
//import org.junit.jupiter.api.assertThrows
//import java.math.BigInteger
//import java.security.KeyPair
//import java.security.KeyPairGenerator
//import java.security.interfaces.RSAPublicKey
//import java.util.*
//
//@DisplayName("JjwtVerifyAdapter ?�스??)
//class JjwtVerifyAdapterTest {
//
//    private lateinit var kakaoOIDCProperties: KakaoOIDCProperties
//    private lateinit var jjwtVerifyAdapter: JjwtVerifyAdapter
//    private lateinit var keyPair: KeyPair
//
//    private val payload = OIDCDecodePayload(
//        iss = "https://kauth.kakao.com",
//        aud = "test-audience",
//        sub = "test-sub",
//        email = "test@example.com",
//        nickname = "고동??
//    )
//
//    @BeforeEach
//    fun setUp() {
//        kakaoOIDCProperties = KakaoOIDCProperties(
//            issuer = "https://kauth.kakao.com",
//            audience = "test-audience",
//            cacheKey = "kakao:oidc:public-keys"
//        )
//        jjwtVerifyAdapter = JjwtVerifyAdapter(kakaoOIDCProperties)
//
//        // ?�스?�용 RSA ?????�성
//        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
//        keyPairGenerator.initialize(2048)
//        keyPair = keyPairGenerator.generateKeyPair()
//    }
//
//    @Test
//    @DisplayName("?�효???�이로드�?검증한??)
//    fun verifyPayLoad_validPayload_success() {
//        // when & then - ?�외가 발생?��? ?�아????
//        jjwtVerifyAdapter.verifyPayLoad(payload)
//    }
//
//    @Test
//    @DisplayName("issuer가 ?�치?��? ?�으�?OIDCInvalidException??발생?�킨??)
//    fun verifyPayLoad_invalidIssuer_throwsException() {
//        val badPayload = OIDCDecodePayload(
//            iss = "https://dsko.dsko.com",
//            aud = "test-audience",
//            sub = "test-sub",
//            email = "test@example.com",
//            nickname = "고동??
//        )
//        // when & then
//        assertThrows<BaseException> {
//            jjwtVerifyAdapter.verifyPayLoad(badPayload)
//        }.also { exception ->
//            assertThat(exception.message).contains("?�큰??issuer가 ?�치?��? ?�습?�다")
//            assertThat(exception.message).contains("https://dsko.dsko.com")
//        }
//    }
//
//    @Test
//    @DisplayName("audience가 ?�치?��? ?�으�?OIDCInvalidException??발생?�킨??)
//    fun verifyPayLoad_invalidAudience_throwsException() {
//        val badPayload = OIDCDecodePayload(
//            iss = "https://kauth.kakao.com",
//            aud = "bad-audience",
//            sub = "test-sub",
//            email = "test@example.com",
//            nickname = "고동??
//        )
//        // when & then
//        assertThrows<BaseException> {
//            jjwtVerifyAdapter.verifyPayLoad(badPayload)
//        }.also { exception ->
//            assertThat(exception.message).contains("?�큰??audience가 ?�치?��? ?�습?�다")
//            assertThat(exception.message).contains("bad-audience")
//        }
//    }
//
//    @Test
//    @DisplayName("?�효???�명?�로 ?�큰??검증한??)
//    fun verifySignature_validToken_success() {
//        // given
//        val token = createValidJwtToken()
//        val rsaPublicKey = keyPair.public as RSAPublicKey
//        val modulus = encodeBase64Url(rsaPublicKey.modulus)
//        val exponent = encodeBase64Url(rsaPublicKey.publicExponent)
//
//        // when
//        val result = jjwtVerifyAdapter.verifySignature(token, modulus, exponent)
//
//        // then
//        assertThat(result).isNotNull()
//        assertThat(result.payload.subject).isEqualTo("test-sub")
//    }
//
//    @Test
//    @DisplayName("만료???�큰?� OIDCExpiredException??발생?�킨??)
//    fun verifySignature_expiredToken_throwsException() {
//        // given
//        val expiredToken = createExpiredJwtToken()
//        val rsaPublicKey = keyPair.public as RSAPublicKey
//        val modulus = encodeBase64Url(rsaPublicKey.modulus)
//        val exponent = encodeBase64Url(rsaPublicKey.publicExponent)
//
//        // when & then
//        assertThrows<BaseException> {
//            jjwtVerifyAdapter.verifySignature(expiredToken, modulus, exponent)
//        }
//    }
//
//    @Test
//    @DisplayName("?�못??modulus ?�식?� InvalidEncodingException??발생?�킨??)
//    fun verifySignature_invalidModulusEncoding_throwsException() {
//        // given
//        val token = createValidJwtToken()
//        val invalidModulus = "invalid-base64-url-encoding!!!"
//        val rsaPublicKey = keyPair.public as RSAPublicKey
//        val exponent = encodeBase64Url(rsaPublicKey.publicExponent)
//
//        // when & then
//        assertThrows<BaseException> {
//            jjwtVerifyAdapter.verifySignature(token, invalidModulus, exponent)
//        }
//    }
//
//    @Test
//    @DisplayName("?�못??exponent ?�식?� InvalidEncodingException??발생?�킨??)
//    fun verifySignature_invalidExponentEncoding_throwsException() {
//        // given
//        val token = createValidJwtToken()
//        val rsaPublicKey = keyPair.public as RSAPublicKey
//        val modulus = encodeBase64Url(rsaPublicKey.modulus)
//        val invalidExponent = "invalid-base64-url-encoding!!!"
//
//        // when & then
//        assertThrows<BaseException> {
//            jjwtVerifyAdapter.verifySignature(token, modulus, invalidExponent)
//        }
//    }
//
//    @Test
//    @DisplayName("RSA ???�성 ?�패 ???�절???�외�?발생?�킨??)
//    fun verifySignature_invalidKeySpec_throwsException() {
//        // given
//        val token = createValidJwtToken()
//        // ?�못???�식??modulus?� exponent (?�무 짧�? 바이??배열)
//        val invalidModulus = Base64.getUrlEncoder().withoutPadding().encodeToString(byteArrayOf(1, 2, 3))
//        val invalidExponent = Base64.getUrlEncoder().withoutPadding().encodeToString(byteArrayOf(1, 2, 3))
//
//        // when & then
//        assertThrows<BaseException> {
//            jjwtVerifyAdapter.verifySignature(token, invalidModulus, invalidExponent)
//        }
//    }
//
//    private fun createValidJwtToken(): String {
//        return Jwts.builder()
//            .subject("test-sub")
//            .issuer("https://kauth.kakao.com")
//            .audience().add("test-audience").and()
//            .issuedAt(Date())
//            .expiration(Date(System.currentTimeMillis() + 3600000)) // 1?�간 ??
//            .claim("email", "test@example.com")
//            .signWith(keyPair.private)
//            .compact()
//    }
//
//    private fun createExpiredJwtToken(): String {
//        val pastDate = Date(System.currentTimeMillis() - 3600000) // 1?�간 ??
//        return Jwts.builder()
//            .subject("test-sub")
//            .issuer("https://kauth.kakao.com")
//            .audience().add("test-audience").and()
//            .issuedAt(pastDate)
//            .expiration(pastDate)
//            .claim("email", "test@example.com")
//            .signWith(keyPair.private)
//            .compact()
//    }
//
//    private fun encodeBase64Url(bigInteger: BigInteger): String {
//        val bytes = bigInteger.toByteArray()
//        val positiveBytes = if (bytes[0] == 0.toByte()) bytes.sliceArray(1 until bytes.size) else bytes
//        return Base64.getUrlEncoder().withoutPadding().encodeToString(positiveBytes)
//    }
//}
//
