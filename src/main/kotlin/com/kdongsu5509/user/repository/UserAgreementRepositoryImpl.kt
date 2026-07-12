package com.kdongsu5509.user.repository

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.repository.TermJpaEntity
import com.kdongsu5509.terms.service.TermRepository
import com.kdongsu5509.user.exception.UserException
import com.kdongsu5509.user.repository.jpa.SpringDataUserAgreementRepository
import com.kdongsu5509.user.repository.jpa.SpringDataUserRepository
import com.kdongsu5509.user.repository.jpa.UserAgreementJpaEntity
import org.springframework.stereotype.Component
import java.util.*

@Component
class UserAgreementRepositoryImpl(
    private val userRepository: SpringDataUserRepository,
    private val termRepository: TermRepository,
    private val userAgreementRepository: SpringDataUserAgreementRepository
) : UserAgreementRepository {

    override fun save(userId: UUID, id: Long) {
        val userEntity = userRepository.findById(userId).orElseThrow {
            UserException.USER_NOT_FOUND.throwIt()
        }
        val term = termRepository.findById(id) ?: TermException.TERM_NOT_FOUND.throwIt()

        userAgreementRepository.save(
            UserAgreementJpaEntity(userEntity, TermJpaEntity.from(term))
        )
    }

    override fun saveAll(userId: UUID, ids: List<Long>) {
        val userEntity = userRepository.findById(userId).orElseThrow {
            UserException.USER_NOT_FOUND.throwIt()
        }
        // P1(방향 a): 전달받은 ids에 해당하는 약관만 조회해 그 목록만 저장한다.
        // (이전 구현은 ids를 무시하고 findActiveAll() 전체를 저장하는 결함이 있었다 — C2)
        val submittedTerms = ids.map { id ->
            termRepository.findById(id) ?: TermException.TERM_NOT_FOUND.throwIt()
        }

        userAgreementRepository.saveAll(
            submittedTerms.map {
                UserAgreementJpaEntity(userEntity, TermJpaEntity.from(it))
            }
        )
    }
}
