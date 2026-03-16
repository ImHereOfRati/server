package com.kdongsu5509.user.application.service.term

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.TermErrorCode
import com.kdongsu5509.user.adapter.`in`.web.terms.dto.NewTermDefinitionRequest
import com.kdongsu5509.user.application.port.`in`.terms.CreateTermsDefinitionUseCase
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
) : CreateTermsDefinitionUseCase {

    override fun createNewTermsDefinition(newTermDefinitionRequest: NewTermDefinitionRequest) {
        val name = newTermDefinitionRequest.termsName
        val type = newTermDefinitionRequest.termsType
        val required = newTermDefinitionRequest.isRequired

        validateTermsDefinitionUniqueness(name, type)

        termsDefinitionSavePort.saveTermDefinition(
            name, type, required
        )
    }

    private fun validateTermsDefinitionUniqueness(termsName: String, termsType: TermsTypes) {
        val checkExistence = termsDefinitionLoadPort.checkExistence(termsName, termsType)
        if (checkExistence) {
            throw BusinessException(TermErrorCode.TERM_DEFINITION_ALREADY_EXIST)
        }
    }
}