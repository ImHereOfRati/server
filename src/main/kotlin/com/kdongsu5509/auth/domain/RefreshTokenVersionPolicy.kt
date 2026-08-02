package com.kdongsu5509.auth.domain

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.support.exception.throwIt

object RefreshTokenVersionPolicy {

    fun assertMatches(currentVersion: Long, tokenVersion: Long) {
        if (currentVersion != tokenVersion) {
            AuthException.IMHERE_INVALID_TOKEN.throwIt()
        }
    }
}
