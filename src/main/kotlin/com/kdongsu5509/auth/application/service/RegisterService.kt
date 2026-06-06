package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.application.ImHereJwtToken
import com.kdongsu5509.auth.application.JwtTokenClaims
import com.kdongsu5509.auth.application.OIDCUserInfo
import com.kdongsu5509.auth.application.port.`in`.RegisterUseCase
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.port.out.OIDCVerifyPort
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterService(
    private val oidcVerifyPort: OIDCVerifyPort,
    private val userRepository: UserRepository,
    private val tokenProviderPort: ImHereTokenProviderPort
) : RegisterUseCase {

    @Transactional
    override fun register(provider: OAuth2Provider, idToken: String): ImHereJwtToken {
        val userInformation = verifyOIDCToken(provider, idToken)
        val newUser = saveNewUser(userInformation.email, userInformation.nickname, provider)

        val newUserClaims = JwtTokenClaims.fromUser(newUser)
        return tokenProviderPort.issue(newUserClaims)
    }

    private fun verifyOIDCToken(provider: OAuth2Provider, idToken: String): OIDCUserInfo {
        return oidcVerifyPort.verify(provider, idToken)
    }

    private fun saveNewUser(email: String, nickname: String, provider: OAuth2Provider): User {
        val isExistingEmail = userRepository.existsByEmail(email)
        val newUser = User.createWithPendingStatus(email, nickname, provider)
        newUser.validateDuplicateEmailAllowed(isExistingEmail)
        return userRepository.save(newUser)
    }
}
