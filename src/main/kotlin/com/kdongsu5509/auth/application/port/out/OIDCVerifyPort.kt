package com.kdongsu5509.auth.application.port.out

import com.kdongsu5509.auth.application.OIDCUserInfo
import com.kdongsu5509.auth.domain.OAuth2Provider

interface OIDCVerifyPort {
    fun verify(provider: OAuth2Provider, idToken: String): OIDCUserInfo
}
