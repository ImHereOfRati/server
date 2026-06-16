package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.`in`.LoginUseCase
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.port.out.OIDCVerifyPort
import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.auth.application.service.dto.OIDCUserInfo
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LoginService(
    private val oidcVerifyPort: OIDCVerifyPort,
    private val userRepository: UserRepository,
    private val tokenProviderPort: ImHereTokenProviderPort
) : LoginUseCase {

    @Transactional
    override fun login(provider: OAuth2Provider, idToken: String, nonce: String?): ImHereJwtToken {
        val userInformation = verifyOIDCToken(provider, idToken, nonce)
        val user = userRepository.findByEmail(userInformation.email) ?: AuthException.USER_NOT_REGISTER.throwIt()

        when (user.status) {
            UserStatus.BLOCKED -> AuthException.USER_DISABLED.throwIt()
            UserStatus.WITHDRAWN -> AuthException.USER_WITHDRAWN.throwIt()
            UserStatus.PENDING, UserStatus.ACTIVE -> {
                val newUserClaims = JwtTokenClaims.fromUser(user)
                return tokenProviderPort.issue(newUserClaims)
            }
        }
    }

    private fun verifyOIDCToken(provider: OAuth2Provider, idToken: String, nonce: String?): OIDCUserInfo {
        return oidcVerifyPort.verify(provider, idToken, nonce)
    }
}
