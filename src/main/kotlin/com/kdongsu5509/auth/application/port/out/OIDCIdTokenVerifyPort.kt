package com.kdongsu5509.auth.application.port.out

import com.kdongsu5509.auth.application.service.dto.OIDCDecodePayload
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws

interface OIDCIdTokenVerifyPort {
    fun getKid(token: String): String

    fun verifyPayLoad(
        payload: OIDCDecodePayload,
        allowedIssuers: Collection<String>,
        allowedAudiences: Collection<String>,
        nonce: String
    )

    fun verifySignature(token: String, modulus: String, exponent: String): Jws<Claims>
}
