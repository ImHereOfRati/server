package com.kdongsu5509.terms.repository

import com.kdongsu5509.terms.domain.Term
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.service.TermRepository
import org.springframework.stereotype.Component

/**
 * TODO : 나중에 사용자 메서드 findAll()에 대해서 캐싱을 추가하여야 합니다.
 * @Cacheable 과 @CacheEvict에 대해 학습 후 추가하세요
 */

@Component
class TermPersistenceAdapter(
    private val termMapper: TermMapper,
    private val termRepository: SpringDataTermRepository
) : TermRepository {
    override fun save(term: Term): Term {
        val entity = termMapper.toEntity(term)
        val savedEntity = termRepository.save(entity)
        return termMapper.toDomain(savedEntity)!!
    }

    override fun findLatestByType(type: TermTypes): Term? {
        val entity = termRepository.findLatestByType(type)
        return termMapper.toDomain(entity)
    }

    override fun findAll(): List<Term> = termRepository.findAll()
        .map { termMapper.toDomain(it)!! }
        .toList()

    override fun findActiveAll() = termRepository.findActiveAll()
        .map { termMapper.toDomain(it)!! }
        .toList()

    override fun findById(id: Long): Term? {
        val foundTermEntity: TermJpaEntity? = termRepository.findById(id).orElse(null)
        return termMapper.toDomain(foundTermEntity)
    }
}
