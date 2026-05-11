package com.kdongsu5509.user.application.service.user.auth

import com.kdongsu5509.user.application.dto.AuthenticationProcessResult
import com.kdongsu5509.user.application.dto.OIDCUserInfo
import com.kdongsu5509.user.application.port.`in`.user.AuthenticateWithOidcUseCase
import com.kdongsu5509.user.application.port.`in`.user.VerifyOIDCUseCase
import com.kdongsu5509.user.application.port.out.user.oauth.OIDCVerifyPort
import com.kdongsu5509.user.domain.user.OAuth2Provider
import org.springframework.stereotype.Component

@Component
class OIDCAuthenticationService(
    private val oidcVerifyPort: OIDCVerifyPort,
    private val userLoginService: UserLoginService,
) : AuthenticateWithOidcUseCase, VerifyOIDCUseCase {

    override fun authenticate(idToken: String, provider: OAuth2Provider): AuthenticationProcessResult {
        val userResponse = verifyOIDC(idToken, provider)
        return userLoginService.loginOrRegister(userResponse.email, userResponse.nickname, provider)
    }

    override fun verifyOIDC(oidc: String, oAuth2Provider: OAuth2Provider): OIDCUserInfo {
        return oidcVerifyPort.verify(oidc)
    }
}
