package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.application.port.`in`.RegisterUseCase
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.port.out.OIDCVerifyPort
import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.auth.application.service.dto.OIDCUserInfo
import com.kdongsu5509.auth.domain.LoginEligibilityPolicy
import com.kdongsu5509.user.api.RegisterUserCommand
import com.kdongsu5509.user.api.UserLookupContract
import com.kdongsu5509.user.api.UserRegistrationContract
import com.kdongsu5509.user.api.UserResult
import com.kdongsu5509.user.domain.EmailRegistrationPolicy
import com.kdongsu5509.user.domain.OAuth2Provider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RegisterService(
    private val oidcVerifyPort: OIDCVerifyPort,
    private val userLookupContract: UserLookupContract,
    private val userRegistrationContract: UserRegistrationContract,
    private val tokenProviderPort: ImHereTokenProviderPort
) : RegisterUseCase {

    @Transactional
    override fun register(provider: OAuth2Provider, idToken: String, nonce: String?): ImHereJwtToken {
        val userInformation = verifyOIDCToken(provider, idToken, nonce)
        val newUser = saveNewUser(userInformation.email, userInformation.nickname, userInformation.sub, provider)

        val newUserClaims = JwtTokenClaims.fromUser(newUser)
        return tokenProviderPort.issue(newUserClaims)
    }

    private fun verifyOIDCToken(provider: OAuth2Provider, idToken: String, nonce: String?): OIDCUserInfo {
        return oidcVerifyPort.verify(provider, idToken, nonce)
    }

    private fun saveNewUser(email: String, nickname: String, sub: String?, provider: OAuth2Provider): UserResult {
        val existingUser = userLookupContract.findByEmailOrNull(email)

        // 기존 계정이 있으면 로그인 자격 정책으로 재가입 가능 여부를 판정한다.
        // (신규 계정이면 existingUser == null 이므로 통과, 중복 검증은 아래에서 수행)
        existingUser?.let { LoginEligibilityPolicy.assertLoginable(it.status) }

        EmailRegistrationPolicy.assertNotDuplicated(existingUser != null)
        return userRegistrationContract.register(
            RegisterUserCommand(
                email = email,
                nickname = nickname,
                oauthProvider = provider,
                oidcSubject = sub,
            )
        )
    }
}
