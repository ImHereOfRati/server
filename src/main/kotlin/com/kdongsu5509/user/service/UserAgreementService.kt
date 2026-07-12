package com.kdongsu5509.user.service

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.service.TermService
import com.kdongsu5509.user.domain.Consent
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.UserAgreementRepository
import com.kdongsu5509.user.repository.UserRepository
import com.kdongsu5509.user.service.dto.MultiTermsConsentCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserAgreementService(
    private val userRepository: UserRepository,
    private val termService: TermService,
    private val userAgreementRepository: UserAgreementRepository,
) : UserAgreementUseCase {

    @Transactional
    override fun consentAll(email: String, multiTermsConsentCommand: MultiTermsConsentCommand): User {
        val activeTerms = termService.findEffectiveTerms()

        val consent = Consent(
            multiTermsConsentCommand.consents.map { Consent.Item(it.id, it.isAgreed) }
        )
        consent.validateAgainst(activeTerms)
        consent.assertRequiredAgreed(activeTerms)

        val user = userRepository.findByEmail(email) ?: UserException.USER_NOT_FOUND.throwIt()

        userAgreementRepository.saveAll(user.id!!, consent.agreedTermIds())

        val activeUser = user.activate()
        userRepository.update(activeUser)

        return activeUser
    }

    @Transactional
    override fun consent(email: String, id: Long) {
        val activeTermIds = termService.findEffectiveTerms().map { it.id }.toSet()
        if (id !in activeTermIds) {
            TermException.TERM_NOT_FOUND.throwIt()
        }

        val user = userRepository.findByEmail(email) ?: UserException.USER_NOT_FOUND.throwIt()
        userAgreementRepository.save(user.id!!, id)
    }
}
