package com.kdongsu5509.user.service

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.application.TermService
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.UserAgreementDao
import com.kdongsu5509.user.repository.UserDao
import com.kdongsu5509.user.service.dto.MultiTermsConsentCommand
import com.kdongsu5509.user.service.dto.MultiTermsConsentCommand.TermConsentCommand
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserAgreementService(
    private val userDao: UserDao,
    private val termService: TermService,
    private val userAgreementDao: UserAgreementDao,
) {

    @Transactional
    fun consentAll(email: String, multiTermsConsentCommand: MultiTermsConsentCommand): User {
        val consents = multiTermsConsentCommand.consents
        verifyRequiredTerms(consents)

        val user = userDao.findByEmail(email) ?: UserException.USER_NOT_FOUND.throwIt()

        val agreedIds = consents.filter { it.isAgreed }.map { it.id }
        userAgreementDao.saveAll(user.id!!, agreedIds)

        user.activate()
        userDao.activate(user.id)

        return user
    }

    @Transactional
    fun consent(email: String, id: Long) {
        val user = userDao.findByEmail(email) ?: UserException.USER_NOT_FOUND.throwIt()
        userAgreementDao.save(user.id!!, id)
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
