package com.kdongsu5509.auth.application.port.out

import com.kdongsu5509.auth.application.JwtTokenClaims

interface ImHereTokenParserPort {
    fun parse(token: String): JwtTokenClaims
    fun validate(token: String): Boolean
}
