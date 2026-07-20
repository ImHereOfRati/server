package com.kdongsu5509.user.repository

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.repository.SpringDataTermRepository
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.jpa.SpringDataUserAgreementRepository
import com.kdongsu5509.user.repository.jpa.SpringDataUserRepository
import com.kdongsu5509.user.repository.jpa.UserAgreementJpaEntity
import org.springframework.stereotype.Component
import java.util.*

@Component
class UserAgreementRepositoryImpl(
    private val userRepository: SpringDataUserRepository,
    private val termRepository: SpringDataTermRepository,
    private val userAgreementRepository: SpringDataUserAgreementRepository,
) : UserAgreementRepository {

    override fun save(userId: UUID, id: Long) {
        val userEntity = userRepository.findById(userId).orElseThrow {
            UserException.USER_NOT_FOUND.throwIt()
        }
        val term = termRepository.findById(id).orElseThrow {
            TermException.TERM_NOT_FOUND.throwIt()
        }

        userAgreementRepository.save(
            UserAgreementJpaEntity(userEntity, term)
        )
    }

    override fun saveAll(userId: UUID, ids: List<Long>) {
        val userEntity = userRepository.findById(userId).orElseThrow {
            UserException.USER_NOT_FOUND.throwIt()
        }
        val submittedTerms = ids.map { id ->
            termRepository.findById(id).orElseThrow {
                TermException.TERM_NOT_FOUND.throwIt()
            }
        }

        userAgreementRepository.saveAll(
            submittedTerms.map { UserAgreementJpaEntity(userEntity, it) }
        )
    }
}
