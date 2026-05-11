package com.kdongsu5509.user.application.service.user.auth

import com.kdongsu5509.user.application.dto.AuthenticationProcessResult
import com.kdongsu5509.user.application.dto.JwtTokenClaims
import com.kdongsu5509.user.application.port.out.user.UserLoadPort
import com.kdongsu5509.user.application.port.out.user.UserSavePort
import com.kdongsu5509.user.application.port.out.user.auth.ImHereTokenProviderPort
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.User
import com.kdongsu5509.user.domain.user.UserStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserLoginService(
    private val userLoadPort: UserLoadPort,
    private val userSavePort: UserSavePort,
    private val tokenProvider: ImHereTokenProviderPort
) {

    fun loginOrRegister(email: String, nickname: String, provider: OAuth2Provider): AuthenticationProcessResult {
        val user = userLoadPort.findByEmail(email) ?: registerNewUser(email, nickname, provider)

        val jwtToken = tokenProvider.issue(JwtTokenClaims.from(user))

        return AuthenticationProcessResult(
            isNewUser = user.status == UserStatus.PENDING,
            accessToken = jwtToken.accessToken,
            refreshToken = jwtToken.refreshToken
        )
    }

    private fun registerNewUser(email: String, nickname: String, provider: OAuth2Provider): User {
        val newUser = User.createWaitingForAgreementUser(email, nickname, provider)
        return userSavePort.save(newUser)
    }
}
