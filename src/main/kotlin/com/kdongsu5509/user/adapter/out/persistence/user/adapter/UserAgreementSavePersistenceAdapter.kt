package com.kdongsu5509.user.adapter.out.persistence.user.adapter

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.TermErrorCode
import com.kdongsu5509.support.exception.UserErrorCode
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.SpringDataTermsVersionRepository
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsVersionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserAgreementRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserAgreementJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.application.port.out.user.UserAgreementSavePort
import org.springframework.stereotype.Component

@Component
class UserAgreementSavePersistenceAdapter(
    private val springDataUserRepository: SpringDataUserRepository,
    private val springDataTermsVersionRepository: SpringDataTermsVersionRepository,
    private val springDataUserAgreementRepository: SpringDataUserAgreementRepository
) : UserAgreementSavePort {

    override fun saveAgreement(userEmail: String, termDefinitionId: Long) {
        val userEntity = findUserEntity(userEmail)
        val termVersion = findTermVersion(termDefinitionId)

        springDataUserAgreementRepository.save(
            UserAgreementJpaEntity(user = userEntity, termsVersion = termVersion)
        )
    }

    override fun saveAgreements(userEmail: String, termDefinitionIds: List<Long>) {
        val userEntity = findUserEntity(userEmail)
        val activeVersions = findTermVersions(termDefinitionIds)

        springDataUserAgreementRepository.saveAll(
            activeVersions.map { version ->
                UserAgreementJpaEntity(user = userEntity, termsVersion = version)
            }
        )
    }

    private fun findUserEntity(userEmail: String): UserJpaEntity =
        springDataUserRepository.findByEmail(userEmail)
            ?: throw BusinessException(UserErrorCode.USER_NOT_FOUND)

    private fun findTermVersion(termDefinitionId: Long): TermsVersionJpaEntity =
        springDataTermsVersionRepository.findActiveVersion(termDefinitionId)
            .orElseThrow { BusinessException(TermErrorCode.TERM_DEFINITION_NOT_FOUND) }

    private fun findTermVersions(termDefinitionIds: List<Long>): List<TermsVersionJpaEntity> {
        val activeVersions = springDataTermsVersionRepository.findAllActiveByDefinitionIds(termDefinitionIds)

        if (activeVersions.size != termDefinitionIds.size) {
            throw BusinessException(TermErrorCode.TERM_DEFINITION_NOT_FOUND)
        }

        return activeVersions
    }
}