package com.kdongsu5509.agreement.service

import com.kdongsu5509.agreement.AgreementException
import com.kdongsu5509.agreement.domain.AgreementStatus
import com.kdongsu5509.agreement.domain.Consent
import com.kdongsu5509.agreement.domain.ConsentChange
import com.kdongsu5509.agreement.repository.AgreementRepository
import com.kdongsu5509.agreement.service.dto.AgreementConsentResult
import com.kdongsu5509.agreement.service.dto.AgreementHistoryResult
import com.kdongsu5509.agreement.service.dto.TermsConsentCommands
import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.terms.TermCatalog
import com.kdongsu5509.terms.TermFact
import com.kdongsu5509.user.api.UserActivationContract
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional(readOnly = true)
class AgreementService(
    private val userActivationContract: UserActivationContract,
    private val agreementRepository: AgreementRepository,
    private val termCatalog: TermCatalog,
) {
    fun findHistory(userId: UUID): List<AgreementHistoryResult> =
        agreementRepository.findHistory(userId)
            .map {
                AgreementHistoryResult(
                    termId = it.termId,
                    action = it.action,
                    occurredAt = it.occurredAt,
                )
            }

    @Transactional
    fun consent(userId: UUID, command: TermsConsentCommands): AgreementConsentResult {
        val consent = command.toConsent()
        val effectiveTerms = termCatalog.findEffectiveTermFacts()
        validateSubmittedTermsAreEffective(consent, effectiveTerms)

        val currentStatuses = findCurrentStatuses(userId)
        val changes = consent.changes(currentStatuses)
        if (changes.isNotEmpty()) agreementRepository.recordChanges(userId, changes)

        val resultingStatuses = currentStatuses + changes.associate { it.termId to it.action }
        val requiredSatisfied = areRequiredTermsSatisfied(effectiveTerms, resultingStatuses)

        if (requiredSatisfied) {
            userActivationContract.activateIfPending(userId)
        }

        return AgreementConsentResult(requiredSatisfied)
    }

    @Transactional
    fun consentToRenewedTerm(userId: UUID, termId: Long) {
        val currentStatuses = findCurrentStatuses(userId)

        if (currentStatuses[termId] == AgreementStatus.CONSENT) return

        val effectiveTerms = termCatalog.findEffectiveTermFacts()
        val renewedTerm = effectiveTerms
            .singleOrNull { it.id == termId }
            ?: AgreementException.TERM_NOT_FOUND.throwIt()

        validateRenewal(renewedTerm, currentStatuses)

        val change = ConsentChange(termId, AgreementStatus.CONSENT)
        agreementRepository.recordChanges(userId, listOf(change))
    }

    @Transactional
    fun withdraw(userId: UUID, termId: Long) {
        val currentStatuses = findCurrentStatuses(userId)
        val effectiveTerms = termCatalog.findEffectiveTermFacts()
        val term = effectiveTerms
            .singleOrNull { it.id == termId }
            ?: AgreementException.TERM_NOT_FOUND.throwIt()

        if (term.isRequired) {
            AgreementException.REQUIRED_AGREEMENT_CANNOT_BE_WITHDRAWN.throwIt()
        }

        val nextStatus = AgreementStatus.next(currentStatuses[termId], false) ?: return
        val change = ConsentChange(termId, nextStatus)
        agreementRepository.recordChanges(userId, listOf(change))
    }

    private fun findCurrentStatuses(userId: UUID): Map<Long, AgreementStatus> =
        agreementRepository.findHistory(userId)
            .associate { it.termId to it.action }

    private fun validateRenewal(
        renewedTerm: TermFact,
        currentStatuses: Map<Long, AgreementStatus>,
    ) {
        val consentedTermIds = currentStatuses
            .filterValues { it == AgreementStatus.CONSENT }
            .keys
        val previouslyAgreedTerms = termCatalog.findTermFacts(consentedTermIds)
        val isRenewal = previouslyAgreedTerms.any {
            it.type == renewedTerm.type && it.version < renewedTerm.version
        }

        if (!isRenewal) AgreementException.TERM_RENEWAL_NOT_REQUIRED.throwIt()
    }

    private fun areRequiredTermsSatisfied(
        effectiveTerms: List<TermFact>,
        statuses: Map<Long, AgreementStatus>,
    ): Boolean =
        effectiveTerms
            .filter { it.isRequired }
            .all { statuses[it.id] == AgreementStatus.CONSENT }

    private fun validateSubmittedTermsAreEffective(consent: Consent, effectiveTerms: List<TermFact>) {
        val effectiveTermIds = effectiveTerms.mapTo(mutableSetOf()) { it.id }
        if (consent.items.any { it.termId !in effectiveTermIds }) {
            AgreementException.TERM_NOT_FOUND.throwIt()
        }
    }
}
