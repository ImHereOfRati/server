package com.kdongsu5509.user.application.port.`in`.user

import com.kdongsu5509.user.application.dto.OIDCUserInfo
import com.kdongsu5509.user.domain.user.OAuth2Provider

interface VerifyOIDCUseCase {
    fun verifyOIDC(oidc: String, oAuth2Provider: OAuth2Provider): OIDCUserInfo
}
