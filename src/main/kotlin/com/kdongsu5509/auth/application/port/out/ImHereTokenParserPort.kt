package com.kdongsu5509.auth.application.port.out

import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims

interface ImHereTokenParserPort {
    fun parse(token: String): JwtTokenClaims
    fun validate(token: String): Boolean
}
