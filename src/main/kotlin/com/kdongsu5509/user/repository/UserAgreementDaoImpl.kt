package com.kdongsu5509.user.repository

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.adapter.out.TermMapper
import com.kdongsu5509.terms.adapter.out.TermPersistenceAdapter
import com.kdongsu5509.user.exception.UserException
import org.springframework.stereotype.Component
import java.util.*

@Component
class UserAgreementDaoImpl(
    private val userRepository: SpringDataUserRepository,
    private val termPersistenceAdapter: TermPersistenceAdapter,
    private val termMapper: TermMapper,
    private val userAgreementRepository: SpringDataUserAgreementRepository
) : UserAgreementDao {

    override fun save(userId: UUID, id: Long) {
        val userEntity = userRepository.findById(userId).orElseThrow {
            UserException.USER_NOT_FOUND.throwIt()
        }
        val term = termPersistenceAdapter.findById(id) ?: TermException.TERM_NOT_FOUND.throwIt()

        userAgreementRepository.save(
            UserAgreementJpaEntity(userEntity, termMapper.toEntity(term))
        )
    }

    override fun saveAll(userId: UUID, ids: List<Long>) {
        val userEntity = userRepository.findById(userId).orElseThrow {
            UserException.USER_NOT_FOUND.throwIt()
        }
        val latestTerms = termPersistenceAdapter.findActiveAll()

        userAgreementRepository.saveAll(
            latestTerms.map {
                UserAgreementJpaEntity(userEntity, termMapper.toEntity(it))
            }
        )
    }
}
