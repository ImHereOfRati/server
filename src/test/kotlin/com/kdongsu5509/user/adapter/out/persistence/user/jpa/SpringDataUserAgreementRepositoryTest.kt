package com.kdongsu5509.user.adapter.out.persistence.user.jpa

import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsDefinitionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsVersionJpaEntity
import com.kdongsu5509.user.domain.terms.TermsTypes
import com.kdongsu5509.user.domain.user.OAuth2Provider
import com.kdongsu5509.user.domain.user.UserStatus
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

        val termsDefinition = TermsDefinitionJpaEntity(
            termsTitle = "서비스 이용약관",
            termsType = TermsTypes.SERVICE,
            isRequired = true
        )
        em.persist(termsDefinition)

        val termsVersion = TermsVersionJpaEntity(
            version = "v1",
            termVersionContent = "약관 내용",
            isActive = true,
            effectiveDate = LocalDateTime.now(),
            terms = termsDefinition
        )
        em.persist(termsVersion)

        val agreement = UserAgreementJpaEntity(
            user = user,
            termsVersion = termsVersion
        )

        // when
        val savedAgreement = agreementRepository.save(agreement)
        em.flush()
        em.clear()

        // then
        val foundAgreement = agreementRepository.findById(savedAgreement.agreementId!!).orElseThrow()
        assertThat(foundAgreement.user.email).isEqualTo(user.email)
        assertThat(foundAgreement.termsVersion.version).isEqualTo("v1")
    }
}
