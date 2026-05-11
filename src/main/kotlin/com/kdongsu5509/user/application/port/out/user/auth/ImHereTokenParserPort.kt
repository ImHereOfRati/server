package com.kdongsu5509.user.application.port.out.user.auth

import com.kdongsu5509.user.application.dto.JwtTokenClaims

interface ImHereTokenParserPort {
    fun parse(token: String): JwtTokenClaims
    fun validate(token: String): Boolean
}
