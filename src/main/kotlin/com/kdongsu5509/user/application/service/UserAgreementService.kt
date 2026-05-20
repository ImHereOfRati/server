package com.kdongsu5509.user.application.service

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.application.TermService
import com.kdongsu5509.user.application.MultiTermsConsentCommand
import com.kdongsu5509.user.application.MultiTermsConsentCommand.TermConsentCommand
import com.kdongsu5509.user.application.port.`in`.UserAgreementUseCase
import com.kdongsu5509.user.application.port.out.UserAgreementPort
import com.kdongsu5509.user.application.port.out.UserUpdatePort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserAgreementService(
    private val userUpdatePort: UserUpdatePort,
    private val termService: TermService,
    private val userAgreementPort: UserAgreementPort,
) : UserAgreementUseCase {

    @Transactional
    override fun consentAll(email: String, multiTermsConsentCommand: MultiTermsConsentCommand) {
        val consents = multiTermsConsentCommand.consents

        verifyRequiredTerms(consents)
        saveAgreements(email, consents)
        userUpdatePort.activate(email)
    }

    @Transactional
    override fun consent(email: String, id: Long) {
        userAgreementPort.save(email, id)
    }

    private fun verifyRequiredTerms(consents: List<TermConsentCommand>) {
        val requiredTerms = termService.findAll(true)
        val consentMap = consents.associate { it.id to it.isAgreed }
        val allRequiredTermsAgreed = requiredTerms.all { consentMap[it.id] == true }

        if (!allRequiredTermsAgreed) {
            TermException.OBLIGATORY_TERM_NOT_AGREED.throwIt()
        }
    }

    private fun saveAgreements(email: String, consents: List<TermConsentCommand>) {
        val agreedIds = consents.filter { it.isAgreed }.map { it.id }
        userAgreementPort.saveAll(email, agreedIds)
    }
}
