package com.kdongsu5509.user.application.service.term

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.TermErrorCode
import com.kdongsu5509.user.adapter.`in`.web.terms.dto.NewTermDefinitionRequest
import com.kdongsu5509.user.application.dto.AlertInformation
import com.kdongsu5509.user.application.port.`in`.terms.CreateTermsDefinitionUseCase
import com.kdongsu5509.user.application.port.out.noti.TermAlertPort
import com.kdongsu5509.user.application.port.out.term.TermsDefinitionLoadPort
import com.kdongsu5509.user.application.port.out.term.TermsDefinitionSavePort
import com.kdongsu5509.user.domain.terms.TermsTypes
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class TermsDefinitionCreateService(
    private val termsDefinitionLoadPort: TermsDefinitionLoadPort,
    private val termsDefinitionSavePort: TermsDefinitionSavePort,
    private val termAlertPort: TermAlertPort,
) : CreateTermsDefinitionUseCase {

    override fun createNewTermsDefinition(newTermDefinitionRequest: NewTermDefinitionRequest) {
        val name = newTermDefinitionRequest.termsName
        val type = newTermDefinitionRequest.termsType
        val required = newTermDefinitionRequest.required

        validateTermsDefinitionUniqueness(name, type)

        termsDefinitionSavePort.saveTermDefinition(
            name, type, required
        )

        termAlertPort.sendAlert(
            AlertInformation(
                senderNickname = "ImHere",
                body = "새로운 약관 $name 이 추가되었습니다",
                receiverEmail = null
            )
        )
    }

    private fun validateTermsDefinitionUniqueness(termsName: String, termsType: TermsTypes) {
        val checkExistence = termsDefinitionLoadPort.checkExistence(termsName, termsType)
        if (checkExistence) {
            throw BusinessException(TermErrorCode.TERM_DEFINITION_ALREADY_EXIST)
        }
    }
}
