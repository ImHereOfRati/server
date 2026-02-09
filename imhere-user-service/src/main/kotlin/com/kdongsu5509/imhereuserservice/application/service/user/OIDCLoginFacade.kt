package com.kdongsu5509.imhereuserservice.application.service.user

import com.kdongsu5509.imhereuserservice.application.dto.AuthenticationProcessResult
import com.kdongsu5509.imhereuserservice.application.dto.OIDCUserInformation
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
    private val jwtTokenProvider: JwtTokenProvider, // JWT 발급 컴포넌트
    private val userSavePort: UserSavePort,
    private val userLoadPort: UserLoadPort,
) : AuthenticateWithOidcUseCase, VerifyOIDCUseCase {

    //OIDC 검증 -> 유저 조회/등록 -> 토큰 발행 및 상태 결정
    override fun authenticate(idToken: String, provider: OAuth2Provider): AuthenticationProcessResult {
        val userInformation = verifyOIDC(idToken, provider)
        // 기존 유저 조회 또는 신규 유저 등록
        val user = getOrRegisterUser(userInformation, provider)

        return convertToAuthenticationResult(user)
    }

    override fun verifyOIDC(oidc: String, oAuth2Provider: OAuth2Provider): OIDCUserInformation {
        return oidcVerificationPort.verifyAndReturnUserInformation(oidc)
    }

    private fun getOrRegisterUser(userInfo: OIDCUserInformation, provider: OAuth2Provider): User {
        val existUser = userLoadPort.findUserByEmailOrNull(userInfo.email)

        // 유저가 없으면 생성(PENDING 상태), 있으면 그대로 반환
        return existUser ?: createNewUser(userInfo, provider)
    }

    private fun createNewUser(userInfo: OIDCUserInformation, provider: OAuth2Provider): User {
        val newUser = User.createPendingUser(
            email = userInfo.email,
            nickname = userInfo.nickname,
            oauthProvider = provider
        )
        return userSavePort.save(newUser)
    }

    private fun convertToAuthenticationResult(user: User): AuthenticationProcessResult {
        val statusCode = decideStatusCode(user)

        val jwtToken = jwtTokenProvider.issueJwtToken(user.email, user.role.toString())

        return AuthenticationProcessResult(
            statusCode = statusCode,
            accessToken = jwtToken.accessToken,
            refreshToken = jwtToken.refreshToken
        )
    }

    private fun decideStatusCode(user: User): Int {
        if (user.status == UserStatus.PENDING) {
            return HttpStatus.CREATED.value() // 201
        }
        return HttpStatus.OK.value()
    }
}