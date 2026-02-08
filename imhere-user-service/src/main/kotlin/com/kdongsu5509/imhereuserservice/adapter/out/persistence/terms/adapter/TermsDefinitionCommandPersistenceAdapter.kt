package com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.adapter

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.jpa.SpringDataTermsDefinitionRepository
import com.kdongsu5509.imhereuserservice.adapter.out.persistence.terms.mapper.TermDefinitionMapper
import com.kdongsu5509.imhereuserservice.application.port.out.term.TermsDefinitionSavePort
import com.kdongsu5509.imhereuserservice.domain.terms.TermsTypes
import org.springframework.cache.annotation.CacheEvict
import org.springframework.stereotype.Component

@Component
class TermsDefinitionCommandPersistenceAdapter(
    private val mapper: TermDefinitionMapper,
    private val springDataTermsDefinitionRepository: SpringDataTermsDefinitionRepository
) :
    TermsDefinitionSavePort {

    @CacheEvict(
        value = ["terms"],
        allEntries = true,
        cacheManager = "redisCacheManager"
    )
    override fun saveTermDefinition(termsName: String, termType: TermsTypes, isRequired: Boolean) {
        springDataTermsDefinitionRepository.save(
            mapper.mapToJpaEntity(
                termsName, termType, isRequired
            )
        )
    }
}