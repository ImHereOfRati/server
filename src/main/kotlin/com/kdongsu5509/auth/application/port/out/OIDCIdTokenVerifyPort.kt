package com.kdongsu5509.auth.application.port.out

import com.kdongsu5509.auth.application.service.dto.OIDCDecodePayload
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws

interface OIDCIdTokenVerifyPort {
    fun getKid(token: String): String

    fun verifyPayLoad(payload: OIDCDecodePayload, issuer: String, audience: String, nonce: String)

    fun verifySignature(token: String, modulus: String, exponent: String): Jws<Claims>
}
