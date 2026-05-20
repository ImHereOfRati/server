package com.kdongsu5509.user.repository

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.terms.adapter.out.TermJpaEntity
import com.kdongsu5509.terms.domain.TermTypes
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
class SpringDataUserAgreementRepositoryTest @Autowired constructor(
    private val agreementRepository: SpringDataUserAgreementRepository,
    private val em: EntityManager
) {

    @Test
    @DisplayName("사용자 약관 동의 이력을 성공적으로 저장하고 조회한다")
    fun save_success() {
        // given
        val user = UserJpaEntity(
            email = "user@example.com",
            nickname = "사용자",
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE
        )
        em.persist(user)

        val term = TermJpaEntity(
            id = null,
            version = 1L,
            type = TermTypes.SERVICE,
            title = "서비스 이용약관",
            content = "약관 내용",
            effectiveDate = LocalDateTime.now(),
            isRequired = true
        )
        em.persist(term)

        val agreement = UserAgreementJpaEntity(
            user = user,
            term = term
        )

        // when
        val savedAgreement = agreementRepository.save(agreement)
        em.flush()
        em.clear()

        // then
        val foundAgreement = agreementRepository.findById(savedAgreement.id!!).orElseThrow()
        assertThat(foundAgreement.user.email).isEqualTo(user.email)
        assertThat(foundAgreement.term.version).isEqualTo(1L)
    }
}
