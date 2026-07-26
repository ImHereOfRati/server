package com.kdongsu5509.terms.service

import com.kdongsu5509.terms.repository.SpringDataTermRepository
import com.kdongsu5509.terms.repository.TermJpaEntity
import com.kdongsu5509.terms.TermCatalog
import com.kdongsu5509.terms.TermFact
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class TermService(
    private val termRepository: SpringDataTermRepository
) : TermCatalog {
    @Transactional
    fun save(command: TermCreateCommand): TermResult {
        val latestVersion = termRepository.findTopByTypeOrderByVersionDesc(command.type)?.version ?: 0L
        val term = TermJpaEntity(
            version = latestVersion + 1L,
            type = command.type,
            title = command.title,
            content = command.content,
            effectiveDate = command.effectiveDate,
            isRequired = command.isRequired,
        )

        return TermResult.from(termRepository.save(term))
    }

    fun findAll(): List<TermResult> = termRepository.findAll()
        .map { TermResult.from(it) }
        .toList()

    fun findAllByIds(ids: Set<Long>): List<TermResult> = termRepository.findAllById(ids)
        .map { TermResult.from(it) }

    fun findEffectiveTerms(): List<TermResult> = termRepository
        .findAllByEffectiveDateLessThanEqual(LocalDateTime.now())
        .groupBy { it.type }
        .values
        .map { terms -> terms.maxBy { it.version } }
        .map { TermResult.from(it) }

    override fun findEffectiveTermFacts(): List<TermFact> = findEffectiveTerms()
        .map { TermFact(it.id, it.version, it.type.name, it.isRequired) }

    override fun findTermFacts(ids: Set<Long>): List<TermFact> = findAllByIds(ids)
        .map { TermFact(it.id, it.version, it.type.name, it.isRequired) }
}
