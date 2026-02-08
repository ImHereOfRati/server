package com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.adapter

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.SpringDataTermsDefinitionRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.mapper.TermDefinitionMapper
import com.kdongsu5509.imhereuserservice.application.port.out.term.TermsDefinitionLoadPort
import com.kdongsu5509.imhereuserservice.domain.terms.TermDefinition
import com.kdongsu5509.imhereuserservice.domain.terms.TermsTypes
import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class TermsDefinitionQueryPersistenceAdapter(
    private val termDefinitionMapper: TermDefinitionMapper,
    private val springDataTermsDefinitionRepository: SpringDataTermsDefinitionRepository
) :
    TermsDefinitionLoadPort {
    override fun checkExistence(termsTitle: String, termsType: TermsTypes): Boolean {
        return springDataTermsDefinitionRepository.existsByTermsTitleAndTermsType(
            termsTitle,
            termsType
        )
    }

    override fun loadTermDefinition(termDefinitionId: Long): TermDefinition {
        val queryResult = springDataTermsDefinitionRepository.findById(termDefinitionId)
            .orElseThrow { BusinessException(ErrorCode.TERM_DEFINITION_NOT_FOUND) }

        return termDefinitionMapper.mapToDomainEntity(queryResult)
    }

    override fun loadAllTermsDefinitions(pageable: Pageable): Page<TermDefinition> {
        return springDataTermsDefinitionRepository.findAll(pageable)
            .map { termDefinitionMapper.mapToDomainEntity(it) }
    }

    @Cacheable(value = ["terms"], key = "'all'", cacheManager = "redisCacheManager")
    override fun loadAllTerms(): List<TermDefinition> {
        return springDataTermsDefinitionRepository.findAll()
            .map { termDefinitionMapper.mapToDomainEntity(it) }
    }
}