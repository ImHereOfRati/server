package com.kdongsu5509.auth.application.port.`in`

import com.kdongsu5509.auth.application.ImHereJwtToken
import com.kdongsu5509.auth.domain.OAuth2Provider

interface LoginUseCase {
    fun login(provider: OAuth2Provider, idToken: String): ImHereJwtToken
}
