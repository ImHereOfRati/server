package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.application.port.`in`.TokenRefreshUseCase
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import org.springframework.stereotype.Service

@Service
class TokenRefreshService(private val tokenProvider: ImHereTokenProviderPort) : TokenRefreshUseCase {
    override fun refresh(refreshToken: String): ImHereJwtToken {
        return tokenProvider.reissueByRefreshToken(refreshToken)
    }
}
