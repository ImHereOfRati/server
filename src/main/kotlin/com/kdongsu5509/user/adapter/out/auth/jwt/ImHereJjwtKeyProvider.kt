package com.kdongsu5509.user.adapter.out.auth.jwt

import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey

/**
 * JWT 서명 및 검증에 사용되는 SecretKey를 관리하는 컴포넌트입니다.
 *
 * [ImHereJwtProperties]에서 읽어온 문자열 기반의 시크릿 값을 HMAC-SHA 알고리즘에 적합한
 * [SecretKey] 객체로 변환하여 지연 로딩(lazy) 방식으로 제공합니다.
 */
@Component
class ImHereJjwtKeyProvider(private val imHereJwtProperties: ImHereJwtProperties) {
    val secretKey: SecretKey by lazy {
        val keyBytes = imHereJwtProperties.secret.toByteArray(StandardCharsets.UTF_8)
        Keys.hmacShaKeyFor(keyBytes)
    }
}
