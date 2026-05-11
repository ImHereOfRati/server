package com.kdongsu5509.user.application.port.out.user.auth

import com.kdongsu5509.user.application.dto.ImHereJwt
import com.kdongsu5509.user.application.dto.JwtTokenClaims

interface ImHereTokenProviderPort {
    fun issue(claims: JwtTokenClaims): ImHereJwt
    fun reissueByRefreshToken(refreshToken: String): ImHereJwt
    fun reissueByEmail(email: String): ImHereJwt
}
