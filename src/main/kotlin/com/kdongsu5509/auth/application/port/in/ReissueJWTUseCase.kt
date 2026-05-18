package com.kdongsu5509.auth.application.port.`in`

import com.kdongsu5509.auth.application.ImHereJwtToken

interface ReissueJWTUseCase {
    fun reissueByRefreshToken(refreshToken: String): ImHereJwtToken
    fun reissueByUserEmail(email: String): ImHereJwtToken
}
