package com.kdongsu5509.imhereuserservice.application.port.`in`.user

import com.kdongsu5509.imhereuserservice.application.dto.UserInformation
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider

interface VerifyOIDCUseCase {
    fun verifyOIDC(oidc: String, oAuth2Provider: OAuth2Provider): UserInformation
}