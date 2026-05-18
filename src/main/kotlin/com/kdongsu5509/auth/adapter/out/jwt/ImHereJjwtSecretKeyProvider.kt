package com.kdongsu5509.auth.adapter.out.jwt

import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey

@Component
class ImHereJjwtSecretKeyProvider(private val imHereJwtProperties: ImHereJwtProperties) {
    val secretKey: SecretKey by lazy {
        var imHereJwtSecretKey = imHereJwtProperties.secret
        val secretKeyBytesValue = imHereJwtSecretKey.toByteArray(StandardCharsets.UTF_8)
        Keys.hmacShaKeyFor(secretKeyBytesValue)
    }
}
