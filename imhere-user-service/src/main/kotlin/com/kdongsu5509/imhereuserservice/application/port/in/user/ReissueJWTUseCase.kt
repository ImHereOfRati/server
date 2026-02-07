package com.kdongsu5509.imhereuserservice.application.port.`in`.user

import com.kdongsu5509.imhereuserservice.application.dto.ImHereJwt

interface ReissueJWTUseCase {
    fun reissue(refreshToken: String): ImHereJwt
}