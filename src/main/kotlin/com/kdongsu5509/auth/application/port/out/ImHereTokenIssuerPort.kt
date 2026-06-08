package com.kdongsu5509.auth.application.port.out

import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims

interface ImHereTokenIssuerPort {
    fun createAccessToken(claims: JwtTokenClaims): String
    fun createRefreshToken(claims: JwtTokenClaims): String
    fun createAdminAccessToken(claims: JwtTokenClaims): String
}
