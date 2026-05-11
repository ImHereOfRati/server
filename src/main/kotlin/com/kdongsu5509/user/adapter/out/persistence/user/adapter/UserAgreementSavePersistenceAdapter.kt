package com.kdongsu5509.user.adapter.out.persistence.user.adapter

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.SpringDataTermsVersionRepository
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsVersionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserAgreementRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserAgreementJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.application.port.out.user.UserAgreementSavePort
import com.kdongsu5509.user.exception.TermError
import com.kdongsu5509.user.exception.UserError
import org.springframework.stereotype.Component

@Component
class UserAgreementSavePersistenceAdapter(
    private val springDataUserRepository: SpringDataUserRepository,
    private val springDataTermsVersionRepository: SpringDataTermsVersionRepository,
    private val springDataUserAgreementRepository: SpringDataUserAgreementRepository
) : UserAgreementSavePort {

    override fun save(userEmail: String, termDefinitionId: Long) {
        val userEntity = findTargetUserByEmail(userEmail)
        val termVersion = findTargetTermVersionById(termDefinitionId)

        springDataUserAgreementRepository.save(
            UserAgreementJpaEntity(user = userEntity, termsVersion = termVersion)
        )
    }

    override fun saveAll(userEmail: String, termDefinitionIds: List<Long>) {
        val userEntity = findTargetUserByEmail(userEmail)
        val activeVersions = findTargetTermVersionsByIds(termDefinitionIds)

        springDataUserAgreementRepository.saveAll(
            activeVersions.map { version ->
                UserAgreementJpaEntity(user = userEntity, termsVersion = version)
            }
        )
    }

    private fun findTargetUserByEmail(userEmail: String): UserJpaEntity =
        springDataUserRepository.findByEmail(userEmail) ?: UserError.USER_NOT_FOUND.throwIt()

    private fun findTargetTermVersionById(termDefinitionId: Long): TermsVersionJpaEntity =
        springDataTermsVersionRepository.findActiveVersion(termDefinitionId)
            ?: TermError.TERM_DEFINITION_NOT_FOUND.throwIt()

    private fun findTargetTermVersionsByIds(termDefinitionIds: List<Long>): List<TermsVersionJpaEntity> {
        val activeVersions = springDataTermsVersionRepository.findAllActiveByDefinitionIds(termDefinitionIds)

        if (activeVersions.size != termDefinitionIds.size) {
            TermError.TERM_DEFINITION_NOT_FOUND.throwIt()
        }

        return activeVersions
    }
}
