package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.ImHereJwtToken
import com.kdongsu5509.auth.application.JwtTokenClaims
import com.kdongsu5509.auth.application.port.`in`.LoginUseCase
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.port.out.OIDCVerifyPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.application.dto.OIDCUserInfo
import com.kdongsu5509.user.application.port.out.UserLoadPort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LoginService(
    private val oidcVerifyPort: OIDCVerifyPort,
    private val userLoadPort: UserLoadPort,
    private val tokenProviderPort: ImHereTokenProviderPort
) : LoginUseCase {

    @Transactional
    override fun login(provider: OAuth2Provider, idToken: String): ImHereJwtToken {
        val userInformation = verifyOIDCToken(provider, idToken)
        val user = userLoadPort.findByEmail(userInformation.email) ?: AuthException.USER_NOT_REGISTER.throwIt()

        val newUserClaims = JwtTokenClaims.fromUser(user)
        return tokenProviderPort.issue(newUserClaims)
    }

    private fun verifyOIDCToken(provider: OAuth2Provider, idToken: String): OIDCUserInfo {
        return oidcVerifyPort.verify(provider, idToken)
    }
}
