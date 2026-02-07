package com.kdongsu5509.imhereuserservice.application.service.user

import com.kdongsu5509.imhereuserservice.application.dto.ImHereJwt
import com.kdongsu5509.imhereuserservice.application.port.`in`.user.ReissueJWTUseCase
import org.springframework.stereotype.Component

@Component
class JwtReissueService(private val jwtTokenProvider: JwtTokenProvider) : ReissueJWTUseCase {
    override fun reissue(refreshToken: String): ImHereJwt {
        return jwtTokenProvider.reissueJwtToken(refreshToken)
    }
}