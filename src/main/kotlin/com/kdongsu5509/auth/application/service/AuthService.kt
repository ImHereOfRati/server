package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.`in`.AuthUseCase
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.port.out.OIDCVerifyPort
import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.auth.application.service.dto.OIDCUserInfo
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.api.RegisterUserCommand
import com.kdongsu5509.user.api.UserLookupContract
import com.kdongsu5509.user.api.UserRegistrationContract
import com.kdongsu5509.user.api.UserResult
import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.UserStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val oidcVerifyPort: OIDCVerifyPort,
    private val tokenProviderPort: ImHereTokenProviderPort,
    private val userLookupContract: UserLookupContract,
    private val userRegistrationContract: UserRegistrationContract
) : AuthUseCase {

    @Transactional
    override fun auth(provider: OAuth2Provider, idToken: String, nonce: String): ImHereJwtToken {
        val userInformation = oidcVerifyPort.verify(provider, idToken, nonce)
        val oidcSubject = userInformation.sub ?: AuthException.OIDC_FORMAT_INVALID.throwIt()
        val user = userLookupContract.findByOidcIdentityOrNull(provider, oidcSubject)
            ?: userLookupContract.findByEmailOrNull(userInformation.email)
            ?: registerNewUser(userInformation, provider)

        if (user.oauthProvider != provider ||
            (user.oidcSubject != null && user.oidcSubject != userInformation.sub)
        ) {
            AuthException.OIDC_FORMAT_INVALID.throwIt()
        }

        validateLoginable(user.status)
        return tokenProviderPort.issue(JwtTokenClaims.fromUser(user))
    }

    private fun registerNewUser(userInformation: OIDCUserInfo, provider: OAuth2Provider): UserResult {
        return userRegistrationContract.register(
            RegisterUserCommand(
                email = userInformation.email,
                nickname = userInformation.nickname,
                oauthProvider = provider,
                oidcSubject = userInformation.sub,
            )
        )
    }

    private fun validateLoginable(status: UserStatus) {
        when (status) {
            UserStatus.BLOCKED -> AuthException.USER_DISABLED.throwIt()
            UserStatus.WITHDRAWN -> AuthException.USER_WITHDRAWN.throwIt()
            UserStatus.PENDING, UserStatus.ACTIVE -> {}
        }
    }
}
