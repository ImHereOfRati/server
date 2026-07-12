package com.kdongsu5509.terms.service

import com.kdongsu5509.terms.domain.Term
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TermService(
    private val termPersistenceAdapter: TermRepository
) {
    @Transactional
    fun save(command: TermCreateCommand): TermResult {
        val previous = termPersistenceAdapter.findLatestByType(command.type)

        return Term.issueNext(
            previous = previous,
            type = command.type,
            title = command.title,
            content = command.content,
            effectiveDate = command.effectiveDate,
            isRequired = command.isRequired,
        ).let { termPersistenceAdapter.save(it) }
            .let { TermResult.from(it) }
    }

    fun findAll(): List<TermResult> = termPersistenceAdapter.findAll()
        .map { TermResult.from(it) }
        .toList()

    fun findEffectiveTerms(): List<TermResult> = termPersistenceAdapter.findActiveAll()
        .map { TermResult.from(it) }
        .toList()
}
