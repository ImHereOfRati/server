package com.kdongsu5509.user.adapter.out.adapter

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.adapter.out.TermMapper
import com.kdongsu5509.terms.adapter.out.TermPersistenceAdapter
import com.kdongsu5509.user.adapter.out.jpa.SpringDataUserAgreementRepository
import com.kdongsu5509.user.adapter.out.jpa.SpringDataUserRepository
import com.kdongsu5509.user.adapter.out.jpa.UserAgreementJpaEntity
import com.kdongsu5509.user.adapter.out.jpa.UserJpaEntity
import com.kdongsu5509.user.application.port.out.UserAgreementPort
import com.kdongsu5509.user.exception.UserException
import org.springframework.stereotype.Component

@Component
class UserAgreementPersistenceAdapter(
    private val userRepository: SpringDataUserRepository,
    private val termPersistenceAdapter: TermPersistenceAdapter,
    private val termMapper: TermMapper,
    private val userAgreementRepository: SpringDataUserAgreementRepository
) : UserAgreementPort {

    override fun save(email: String, id: Long) {
        val userEntity = findUserByEmail(email)
        val term = termPersistenceAdapter.findById(id) ?: TermException.TERM_NOT_FOUND.throwIt()

        userAgreementRepository.save(
            UserAgreementJpaEntity(userEntity, termMapper.toEntity(term))
        )
    }

    override fun saveAll(email: String, ids: List<Long>) {
        val userEntity = findUserByEmail(email)
        val latestTerms = termPersistenceAdapter.findActiveAll()

        userAgreementRepository.saveAll(
            latestTerms.map {
                UserAgreementJpaEntity(userEntity, termMapper.toEntity(it))
            }
        )
    }

    private fun findUserByEmail(userEmail: String): UserJpaEntity =
        userRepository.findByEmail(userEmail) ?: UserException.USER_NOT_FOUND.throwIt()
}
