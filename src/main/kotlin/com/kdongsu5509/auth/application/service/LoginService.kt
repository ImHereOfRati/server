package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.`in`.LoginUseCase
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.port.out.OIDCVerifyPort
import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.auth.application.service.dto.OIDCUserInfo
import com.kdongsu5509.auth.domain.LoginEligibilityPolicy
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.api.UserLookupContract
import com.kdongsu5509.user.domain.OAuth2Provider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LoginService(
    private val oidcVerifyPort: OIDCVerifyPort,
    private val userLookupContract: UserLookupContract,
    private val tokenProviderPort: ImHereTokenProviderPort
) : LoginUseCase {

    @Transactional
    override fun login(provider: OAuth2Provider, idToken: String, nonce: String?): ImHereJwtToken {
        val userInformation = verifyOIDCToken(provider, idToken, nonce)
        val user =
            userLookupContract.findByEmailOrNull(userInformation.email) ?: AuthException.USER_NOT_REGISTER.throwIt()

        LoginEligibilityPolicy.assertLoginable(user.status)

        val newUserClaims = JwtTokenClaims.fromUser(user)
        return tokenProviderPort.issue(newUserClaims)
    }

    private fun verifyOIDCToken(provider: OAuth2Provider, idToken: String, nonce: String?): OIDCUserInfo {
        return oidcVerifyPort.verify(provider, idToken, nonce)
    }
}
