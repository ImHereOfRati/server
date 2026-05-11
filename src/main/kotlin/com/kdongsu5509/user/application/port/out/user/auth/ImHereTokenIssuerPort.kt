package com.kdongsu5509.user.application.port.out.user.auth

import com.kdongsu5509.user.application.dto.JwtTokenClaims

interface ImHereTokenIssuerPort {
    fun createAccessToken(claims: JwtTokenClaims): String
    fun createRefreshToken(claims: JwtTokenClaims): String
    fun createAdminAccessToken(claims: JwtTokenClaims): String
}
