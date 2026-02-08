package com.kdongsu5509.imhereuserservice.application.service.user

import com.kdongsu5509.imhereuserservice.adapter.`in`.web.user.dto.UserTermsConsentRequest
import com.kdongsu5509.imhereuserservice.adapter.`in`.web.user.dto.UserTermsConsentRequest.ConsentDetail
import com.kdongsu5509.imhereuserservice.application.port.`in`.user.AgreementTermUseCase
import com.kdongsu5509.imhereuserservice.application.port.out.term.TermsDefinitionLoadPort
import com.kdongsu5509.imhereuserservice.application.port.out.user.UserAgreementSavePort
import com.kdongsu5509.imhereuserservice.application.port.out.user.UserUpdatePort
import com.kdongsu5509.imhereuserservice.domain.terms.TermDefinition
import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service

@Service
@Transactional
class UserTermAgreementService(
    private val userUpdatePort: UserUpdatePort,
    private val termsDefinitionLoadPort: TermsDefinitionLoadPort,
    private val userAgreementSavePort: UserAgreementSavePort,
) : AgreementTermUseCase {
    override fun consentAll(
        username: String,
        userTermsConsentRequest: UserTermsConsentRequest
    ) {
        val consents = userTermsConsentRequest.consents

        verifyAllRequiredTermsAgreed(consents)
        saveRequestedAgreements(consents, username)
        userUpdatePort.activate(username)
    }

    override fun consent(username: String, termDefinitionId: Long) {
        userAgreementSavePort.saveAgreement(
            userEmail = username,
            termDefinitionId = termDefinitionId
        )
    }

    private fun saveRequestedAgreements(
        consents: List<ConsentDetail>,
        username: String
    ) {
        userAgreementSavePort.saveAgreements(
            username,
            consents.filter { it.isAgreed }.map { it.termDefinitionId }.toList()
        )
    }

    private fun verifyAllRequiredTermsAgreed(consents: List<ConsentDetail>) {
        val requiredTerms = loadRequiredTerms()

        val agreedTermIds = consents.filter { it.isAgreed }.map { it.termDefinitionId }.toSet()
        val allRequiredTermsAgreed = requiredTerms.all { it.id in agreedTermIds }

        if (!allRequiredTermsAgreed) {
            throw BusinessException(ErrorCode.OBLIGATORY_TERM_NOT_AGREED)
        }
    }

    private fun loadRequiredTerms(): List<TermDefinition> {
        return termsDefinitionLoadPort
            .loadAllTerms()
            .filter { it.isRequired }
    }
}