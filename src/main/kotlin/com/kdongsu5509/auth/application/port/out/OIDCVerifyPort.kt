package com.kdongsu5509.auth.application.port.out

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.user.application.dto.OIDCUserInfo

interface OIDCVerifyPort {
    fun verify(provider: OAuth2Provider, idToken: String): OIDCUserInfo
}
