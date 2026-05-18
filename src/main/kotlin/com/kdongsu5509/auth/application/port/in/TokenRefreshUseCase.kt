package com.kdongsu5509.auth.application.port.`in`

import com.kdongsu5509.auth.application.ImHereJwtToken

interface TokenRefreshUseCase {
    fun refresh(refreshToken: String): ImHereJwtToken
}
