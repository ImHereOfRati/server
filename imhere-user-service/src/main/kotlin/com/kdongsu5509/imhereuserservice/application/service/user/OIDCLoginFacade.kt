package com.kdongsu5509.imhereuserservice.application.service.user

import com.kdongsu5509.imhereuserservice.application.dto.AuthenticationProcessResult
import com.kdongsu5509.imhereuserservice.application.dto.UserInformation
import com.kdongsu5509.imhereuserservice.application.port.`in`.user.AuthenticateWithOidcUseCase
import com.kdongsu5509.imhereuserservice.application.port.`in`.user.VerifyOIDCUseCase
import com.kdongsu5509.imhereuserservice.application.port.out.user.UserLoadPort
import com.kdongsu5509.imhereuserservice.application.port.out.user.UserSavePort
import com.kdongsu5509.imhereuserservice.application.port.out.user.oauth.OIDCVerificationPort
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.User
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
@Transactional
class OIDCLoginFacade(
    private val oidcVerificationPort: OIDCVerificationPort,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userSavePort: UserSavePort,
    private val userLoadPort: UserLoadPort,
) : AuthenticateWithOidcUseCase, VerifyOIDCUseCase {
    override fun authenticate(idToken: String, provider: OAuth2Provider): AuthenticationProcessResult {
        val userInformation = verifyOIDC(idToken, provider)
        val (user, isNewAndActive) = getOrRegisterUser(userInformation, provider)
        return convertToAuthenticationResult(user, isNewAndActive)
    }

    override fun verifyOIDC(oidc: String, oAuth2Provider: OAuth2Provider): UserInformation {
        return oidcVerificationPort.verifyAndReturnUserInformation(oidc)
    }

    private fun getOrRegisterUser(userInfo: UserInformation, provider: OAuth2Provider): Pair<User, Boolean> {
        val existUser: User? = userLoadPort.findUserByEmailOrNull(userInfo.email)

        val isExist = existUser != null
        val isActiveUser = isExist && (existUser.status != UserStatus.ACTIVE)

        if (isActiveUser) {
            return existUser to false
        }

        if (isExist) {
            return existUser to true
        }

        // 완전 신규
        val savedUser = createNewUser(userInfo, provider)
        return savedUser to true
    }

    private fun createNewUser(
        userInfo: UserInformation,
        provider: OAuth2Provider
    ): User {
        val newUser = User.createPendingUser(
            email = userInfo.email,
            nickname = userInfo.nickname,
            oauthProvider = provider
        )

        val savedUser = userSavePort.save(newUser)
        return savedUser
    }

    private fun convertToAuthenticationResult(user: User, isNew: Boolean): AuthenticationProcessResult {
        val statusCode = if (isNew) HttpStatus.CREATED.value() else HttpStatus.OK.value()
        val jwtToken = jwtTokenProvider.issueJwtToken(user.email, user.role.toString())

        return AuthenticationProcessResult(
            statusCode = statusCode,
            accessToken = jwtToken.accessToken,
            refreshToken = jwtToken.refreshToken
        )
    }
}