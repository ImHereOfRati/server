package com.kdongsu5509.user.repository

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.repository.SpringDataTermRepository
import com.kdongsu5509.terms.repository.TermJpaEntity
import com.kdongsu5509.terms.repository.TermMapper
import com.kdongsu5509.terms.repository.TermPersistenceAdapter
import com.kdongsu5509.user.domain.UserStatus
import com.kdongsu5509.user.repository.jpa.SpringDataUserAgreementRepository
import com.kdongsu5509.user.repository.jpa.SpringDataUserRepository
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

/**
 * P1(방향 a) 통합 테스트: mock이 아닌 실제 어댑터 경로로 "제출한 것만 저장됨"을 검증한다.
 * 기존 mock-only 검증(UserAgreementServiceTest / UserAgreementRepositoryImplTest)만으로는
 * saveAll이 ids를 무시하고 활성 약관 전체를 저장하던 결함(C2)을 잡지 못했다.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(UserAgreementRepositoryImpl::class, TermPersistenceAdapter::class, TermMapper::class)
class UserAgreementRepositoryImplIntegrationTest @Autowired constructor(
    private val userAgreementRepositoryImpl: UserAgreementRepositoryImpl,
    private val springDataUserRepository: SpringDataUserRepository,
    private val springDataTermRepository: SpringDataTermRepository,
    private val agreementRepository: SpringDataUserAgreementRepository,
    private val em: EntityManager,
) {

    @Test
    @DisplayName("saveAll은 전달받은 약관 ID에 해당하는 동의 이력만 저장한다(선택 약관 미동의는 저장하지 않는다)")
    fun saveAll_saves_only_submitted_terms() {
        // given: 활성 약관 2개(필수 SERVICE, 선택 MARKETING)
        val user = UserJpaEntity(
            email = "user@example.com",
            nickname = "사용자",
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.PENDING
        )
        springDataUserRepository.save(user)

        val requiredTerm = TermJpaEntity(
            id = null, version = 1L, type = TermTypes.SERVICE,
            title = "서비스 이용약관", content = "내용", effectiveDate = LocalDateTime.now(), isRequired = true
        )
        val optionalTerm = TermJpaEntity(
            id = null, version = 1L, type = TermTypes.MARKETING,
            title = "마케팅 약관", content = "내용", effectiveDate = LocalDateTime.now(), isRequired = false
        )
        springDataTermRepository.save(requiredTerm)
        springDataTermRepository.save(optionalTerm)
        em.flush()
        em.clear()

        // when: 선택 약관(MARKETING)은 빼고 필수 약관 ID만 제출
        userAgreementRepositoryImpl.saveAll(user.id!!, listOf(requiredTerm.id!!))
        em.flush()
        em.clear()

        // then: 제출한 1건만 저장되어야 한다(활성 약관 전체 2건이 아니라)
        val agreements = agreementRepository.findAll()
        assertThat(agreements).hasSize(1)
        assertThat(agreements[0].term.id).isEqualTo(requiredTerm.id)
    }
}
