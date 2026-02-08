package com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.adapter

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.SpringDataTermsVersionRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.mapper.TermVersionMapper
import com.kdongsu5509.imhereuserservice.application.port.out.term.TermsVersionLoadPort
import com.kdongsu5509.imhereuserservice.domain.terms.TermVersion
import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class TermsVersionQueryPersistenceAdapter(
    private val termVersionMapper: TermVersionMapper,
    private val springDataTermsVersionRepository: SpringDataTermsVersionRepository
) :
    TermsVersionLoadPort {
    @Cacheable(
        value = ["term-versions"],
        key = "#termDefinitionId",
        cacheManager = "redisCacheManager"
    )
    override fun loadSpecificActiveTermVersion(termDefinitionId: Long): TermVersion {
        val queryResult = springDataTermsVersionRepository.findActiveVersion(termDefinitionId)

        if (queryResult.isEmpty) {
            throw BusinessException(ErrorCode.TERM_DEFINITION_NOT_FOUND)
        }

        return termVersionMapper.mapToDomainEntity(queryResult.get())
    }
}