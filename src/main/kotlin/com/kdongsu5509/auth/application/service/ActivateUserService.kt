package com.kdongsu5509.auth.application.service

import com.kdongsu5509.agreement.service.AgreementService
import com.kdongsu5509.agreement.service.dto.TermsConsentCommands
import com.kdongsu5509.auth.AuthException
import com.kdongsu5509.auth.application.port.`in`.ActivateUserUseCase
import com.kdongsu5509.auth.application.port.out.ImHereTokenProviderPort
import com.kdongsu5509.auth.application.service.dto.ImHereJwtToken
import com.kdongsu5509.auth.application.service.dto.UserActivationCommand
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.support.exception.throwIt
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Deprecated(
    message = "약관 동의 이벤트 기반 사용자 활성화 흐름으로 대체될 예정입니다.",
)
class ActivateUserService(
    private val agreementService: AgreementService,
    private val tokenProviderPort: ImHereTokenProviderPort
) : ActivateUserUseCase {

    @Transactional
    override fun activate(command: UserActivationCommand, userStatus: String): ImHereJwtToken {
        if (userStatus != UserStatus.PENDING.name) AuthException.IMHERE_ALREADY_ACTIVE.throwIt()

        val consentsCommand = TermsConsentCommands(
            consents = command.consents.map {
                TermsConsentCommands.TermConsentCommand(id = it.id, isAgreed = it.isAgreed)
            }
        )

        agreementService.consent(command.userId, consentsCommand)
            .requireRequiredAgreementsSatisfied()
        return tokenProviderPort.reissueByEmail(command.email)
    }
}
