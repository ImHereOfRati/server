package com.kdongsu5509.user.application.service.user.auth

import com.kdongsu5509.user.application.dto.ImHereJwt
import com.kdongsu5509.user.application.port.`in`.user.ReissueJWTUseCase
import com.kdongsu5509.user.application.port.out.user.auth.ImHereTokenProviderPort
import org.springframework.stereotype.Component

@Component
class JwtReissueService(private val tokenProvider: ImHereTokenProviderPort) : ReissueJWTUseCase {
    override fun reissueByRefreshToken(refreshToken: String): ImHereJwt {
        return tokenProvider.reissueByRefreshToken(refreshToken)
    }

    override fun reissueByUserEmail(email: String): ImHereJwt {
        return tokenProvider.reissueByEmail(email)
    }
}
