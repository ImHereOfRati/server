package com.kdongsu5509.user.service

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.application.TermService
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.UserAgreementRepository
import com.kdongsu5509.user.repository.UserRepository
import com.kdongsu5509.user.service.dto.MultiTermsConsentCommand
import com.kdongsu5509.user.service.dto.MultiTermsConsentCommand.TermConsentCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserAgreementService(
    private val userRepository: UserRepository,
    private val termService: TermService,
    private val userAgreementRepository: UserAgreementRepository,
) {

    @Transactional
    fun consentAll(email: String, multiTermsConsentCommand: MultiTermsConsentCommand): User {
        val consents = multiTermsConsentCommand.consents
        verifyRequiredTerms(consents)

        val user = userRepository.findByEmail(email) ?: UserException.USER_NOT_FOUND.throwIt()

        val agreedIds = consents.filter { it.isAgreed }.map { it.id }
        userAgreementRepository.saveAll(user.id!!, agreedIds)

        user.activate()
        userRepository.activate(user.id)

        return user
    }

    @Transactional
    fun consent(email: String, id: Long) {
        val user = userRepository.findByEmail(email) ?: UserException.USER_NOT_FOUND.throwIt()
        userAgreementRepository.save(user.id!!, id)
    }

    private fun verifyRequiredTerms(consents: List<TermConsentCommand>) {
        val requiredTerms = termService.findAll(true)
        val consentMap = consents.associate { it.id to it.isAgreed }
        val allRequiredTermsAgreed = requiredTerms.all { consentMap[it.id] == true }

        if (!allRequiredTermsAgreed) {
            TermException.OBLIGATORY_TERM_NOT_AGREED.throwIt()
        }
    }
}
