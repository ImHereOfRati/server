package com.kdongsu5509.auth.application.service

import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.`in`.ActivateUserUseCase
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.auth.application.service.dto.JwtTokenClaims
import com.kdongsu5509.auth.application.service.dto.UserActivationCommand
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.service.UserAgreementService
import com.kdongsu5509.user.service.dto.MultiTermsConsentCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ActivateUserService(
    private val userAgreementService: UserAgreementService,
    private val tokenProviderPort: ImHereTokenProviderPort
) : ActivateUserUseCase {

    @Transactional
    override fun activate(command: UserActivationCommand, userStatus: String): ImHereJwtToken {
        if (userStatus != UserStatus.PENDING.name) AuthException.IMHERE_ALREADY_ACTIVE.throwIt()

        // 1. 약관 동의 처리 및 회원 상태를 ACTIVE로 변경
        val consentsCommand = MultiTermsConsentCommand(
            consents = command.consents.map {
                MultiTermsConsentCommand.TermConsentCommand(id = it.id, isAgreed = it.isAgreed)
            }
        )

        val user = userAgreementService.consentAll(command.email, consentsCommand)

        // 2. ACTIVE 상태가 반영된 새 JWT 토큰 발급
        val newUserClaims = JwtTokenClaims.fromUser(user)
        return tokenProviderPort.issue(newUserClaims)
    }
}
