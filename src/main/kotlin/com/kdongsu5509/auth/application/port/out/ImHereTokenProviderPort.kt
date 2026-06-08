package com.kdongsu5509.auth.application.port.out

import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims

interface ImHereTokenProviderPort {
    fun issue(claims: JwtTokenClaims): ImHereJwtToken
    fun reissueByRefreshToken(refreshToken: String): ImHereJwtToken
    fun reissueByEmail(email: String): ImHereJwtToken
}
