package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.`in`.RegisterUseCase
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.port.out.OIDCVerifyPort
import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.auth.application.service.dto.OIDCUserInfo
import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserStatus
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
        val existingUser = userRepository.findByEmail(email)
        if (existingUser?.status == UserStatus.BLOCKED) {
            AuthException.USER_DISABLED.throwIt()
        }
        val newUser = User.createWithPendingStatus(email, nickname, provider)
        newUser.validateDuplicateEmailAllowed(existingUser != null)
        return userRepository.save(newUser)
    }
}
