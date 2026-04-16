package com.common.testUtil

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.*

object TestJwtBuilder {

    const val KAKAO_HEADER_ALG = "RS256"
    const val KAKAO_HEADER_TYP = "JWT"
    const val KAKAO_HEADER_KID = "test-kid"

    const val KAKAO_PAYLOAD_ISS = "https://kauth.kakao.com"
    const val KAKAO_PAYLOAD_AUD = "bf284f33bfeba9bc59575706d0eb0e9c" // 테스트용 앱 키
    const val KAKAO_PAYLOAD_SUB = "사용자회원번호"
    const val KAKAO_PAYLOAD_EMAIL = "ds.ko@kakao.com"
    const val KAKAO_PAYLOAD_EXP_SECONDS = 3600L

    // application-test.yml 의 jwt.secret 과 동일한 값
    const val TEST_IMHERE_JWT_SECRET = "testSecretKeyForJwtAuthenticationTesting12345678901234567890"

    val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply {
        initialize(2048)  // 최소 2048비트 RSA 키 필요
    }.generateKeyPair()
    val testPublicKey = keyPair.public
    val testPrivateKey = keyPair.private

    private val imHereSecretKey by lazy {
        Keys.hmacShaKeyFor(TEST_IMHERE_JWT_SECRET.toByteArray(StandardCharsets.UTF_8))
    }

    /**
     * 유효한 카카오 규격의 ID 토큰을 생성하는 빌더 메서드입니다.
     */
    fun buildValidIdToken(): String {
        return createKakaoIdToken(KAKAO_PAYLOAD_EMAIL)
    }

    fun buildValidIdTokenWithCustomEmail(email: String): String {
        return createKakaoIdToken(email)
    }

    /**
     * 테스트용 ImHere Access Token을 생성합니다.
     * application-test.yml 의 jwt.secret 으로 서명하므로 JwtTokenUtil 과 동일한 secret을 사용합니다.
     */
    fun buildImHereAccessToken(
        email: String,
        nickname: String,
        role: String = "NORMAL",
        status: String = "PENDING",
        uid: UUID = UUID.randomUUID()
    ): String = createImHereToken("access", email, nickname, role, status, uid, expirationSeconds = 1800L)

    /**
     * 테스트용 ImHere Refresh Token을 생성합니다.
     * application-test.yml 의 jwt.secret 으로 서명하므로 JwtTokenUtil 과 동일한 secret을 사용합니다.
     */
    fun buildImHereRefreshToken(
        email: String,
        nickname: String,
        role: String = "NORMAL",
        status: String = "PENDING",
        uid: UUID = UUID.randomUUID()
    ): String = createImHereToken("refresh", email, nickname, role, status, uid, expirationSeconds = 604800L)

    private fun createImHereToken(
        category: String,
        email: String,
        nickname: String,
        role: String,
        status: String,
        uid: UUID,
        expirationSeconds: Long
    ): String {
        val now = Instant.now()
        return Jwts.builder()
            .setId(UUID.randomUUID().toString())
            .claim("category", category)
            .claim("uid", uid.toString())
            .claim("email", email)
            .claim("nickname", nickname)
            .claim("role", "ROLE_$role")
            .claim("status", status)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plusSeconds(expirationSeconds)))
            .signWith(imHereSecretKey)
            .compact()
    }

    private fun createKakaoIdToken(email: String): String {
        val now = Instant.now()
        val issuedAt = Date.from(now)
        val expiration = Date.from(now.plusSeconds(KAKAO_PAYLOAD_EXP_SECONDS))

        val payload: Map<String, Any> = mapOf(
            "iss" to KAKAO_PAYLOAD_ISS,
            "aud" to KAKAO_PAYLOAD_AUD,
            "sub" to KAKAO_PAYLOAD_SUB,
            "iat" to issuedAt,
            "exp" to expiration,
            "auth_time" to issuedAt,
            "nonce" to UUID.randomUUID().toString(),
            "email" to email
        )

        return Jwts.builder()
            .setHeaderParams(
                mapOf(
                    "typ" to KAKAO_HEADER_TYP,
                    "kid" to KAKAO_HEADER_KID,
                    "alg" to KAKAO_HEADER_ALG
                )
            )
            .setClaims(payload)
            .signWith(testPrivateKey, SignatureAlgorithm.RS256)
            .compact()
    }
}
