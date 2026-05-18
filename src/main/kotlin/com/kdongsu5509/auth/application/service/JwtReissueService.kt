package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.application.ImHereJwtToken
import com.kdongsu5509.auth.application.port.`in`.ReissueJWTUseCase
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import org.springframework.stereotype.Component

@Component
class JwtReissueService(private val tokenProvider: ImHereTokenProviderPort) : ReissueJWTUseCase {
    override fun reissueByRefreshToken(refreshToken: String): ImHereJwtToken {
        return tokenProvider.reissueByRefreshToken(refreshToken)
    }

    override fun reissueByUserEmail(email: String): ImHereJwtToken {
        return tokenProvider.reissueByEmail(email)
    }
}
