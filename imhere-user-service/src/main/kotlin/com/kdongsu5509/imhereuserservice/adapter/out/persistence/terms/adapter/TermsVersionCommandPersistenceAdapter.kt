package com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.adapter

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.SpringDataTermsDefinitionRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.SpringDataTermsVersionRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.TermsDefinitionJpaEntity
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.TermsVersionJpaEntity
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.mapper.TermVersionMapper
import com.kdongsu5509.imhereuserservice.application.port.out.term.TermsVersionSavePort
import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class TermsVersionCommandPersistenceAdapter(
    private val termVersionMapper: TermVersionMapper,
    private val springDataTermsDefinitionRepository: SpringDataTermsDefinitionRepository,
    private val springDataTermsVersionRepository: SpringDataTermsVersionRepository
) :
    TermsVersionSavePort {

    @CacheEvict(
        value = ["term-versions"],
        key = "#termDefinitionId",
        cacheManager = "redisCacheManager"
    )
    override fun saveTermVersion(
        termDefinitionId: Long,
        version: String,
        content: String,
        effectiveDate: LocalDateTime
    ) {
        val queriedTermDefinition = findProperTermDefinition(termDefinitionId)
        deactivePrevTermVersion(termDefinitionId)

        springDataTermsVersionRepository.save(
            createNewTermVersionEntity(
                queriedTermDefinition,
                version,
                content,
                effectiveDate
            )
        )
    }

    private fun createNewTermVersionEntity(
        queriedTermDefinition: TermsDefinitionJpaEntity,
        version: String,
        content: String,
        effectiveDate: LocalDateTime
    ): TermsVersionJpaEntity {
        val termVersionJpaEntity = termVersionMapper.mapToJpaEntity(
            queriedTermDefinition,
            version,
            content,
            effectiveDate
        ).apply {
            this.isActive = true
        }
        return termVersionJpaEntity
    }


    private fun deactivePrevTermVersion(termDefinitionId: Long) {
        springDataTermsVersionRepository.findActiveVersion(termDefinitionId).ifPresent { activeVersion ->
            activeVersion.isActive = false
        }
    }

    private fun findProperTermDefinition(termDefinitionId: Long): TermsDefinitionJpaEntity {
        val queriedTermDefinition = springDataTermsDefinitionRepository.findById(termDefinitionId).orElseThrow {
            throw BusinessException(ErrorCode.TERM_DEFINITION_NOT_FOUND)
        }
        return queriedTermDefinition
    }
}