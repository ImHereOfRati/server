package com.kdongsu5509.imhereuserservice.application.service.user

import com.kdongsu5509.imhereuserservice.application.dto.ImHereJwt

interface JwtTokenProvider {
    fun issueJwtToken(email: String, role: String): ImHereJwt

    fun reissueJwtToken(refreshToken: String): ImHereJwt
}