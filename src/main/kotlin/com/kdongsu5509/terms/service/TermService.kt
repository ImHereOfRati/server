package com.kdongsu5509.terms.service

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.domain.Term
import com.kdongsu5509.terms.repository.TermPersistenceAdapter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TermService(
    private val termPersistenceAdapter: TermPersistenceAdapter
) {
    @Transactional
    fun save(command: TermCreateCommand): TermResult {
        val nextVersion = (termPersistenceAdapter.findLatestByType(command.type)?.version ?: 0L) + 1L

        return Term.createWithVersion(
            type = command.type,
            title = command.title,
            content = command.content,
            effectiveDate = command.effectiveDate,
            isRequired = command.isRequired,
            version = nextVersion
        ).let { termPersistenceAdapter.save(it) }
            .let { TermResult.from(it) }
    }

    @PreAuthorize("hasRole('PENDING')")
    fun findAll(): List<TermResult> = termPersistenceAdapter.findAll()
        .map { TermResult.from(it) }
        .toList()

    fun findAll(isActive: Boolean): List<TermResult> =
        if (isActive)
            termPersistenceAdapter.findActiveAll()
                .map { TermResult.from(it) }
                .toList()
        else TermException.NON_ACTIVE_TERM_NOT_ALLOWED.throwIt()
}
