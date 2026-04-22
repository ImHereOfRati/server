package com.kdongsu5509.user.adapter.out.persistence.terms.jpa

import com.kdongsu5509.user.domain.terms.TermsTypes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@Profile("test")
class SpringDataTermsVersionRepositoryTest @Autowired constructor(
    private val repository: SpringDataTermsVersionRepository,
    private val termsDefinitionRepository: SpringDataTermsDefinitionRepository
) {


    @Test
    @DisplayName("특정 약관의 활성화된(isActive=true) 버전을 조회한다.")
    fun findActiveVersion_Success() {
        // given
        val definition = termsDefinitionRepository.save(
            TermsDefinitionJpaEntity("서비스 이용약관", TermsTypes.SERVICE, true)
        )

        val activeVersion = createActiveTermVersion(definition)
        repository.save(TermsVersionJpaEntity("0.1", "이전 내용", false, LocalDateTime.now().minusDays(1), definition))
        repository.save(activeVersion)

        // when
        val result = repository.findActiveVersion(definition.id!!).get()

        // then
        assertThat(result).isNotNull
        assertThat(result.version).isEqualTo("1.0")
        assertThat(result.isActive).isTrue()
    }

    @Test
    @DisplayName("활성화된 버전이 없으면 비어있는 Optional을 반환한다.")
    fun findActiveVersion_ReturnNull() {
        // given
        val definition = termsDefinitionRepository.save(
            TermsDefinitionJpaEntity("위치 약관", TermsTypes.LOCATION, true)
        )
        repository.save(
            TermsVersionJpaEntity("1.0", "비활성 내용", false, LocalDateTime.now(), definition)
        )

        // when
        val result = repository.findActiveVersion(definition.id!!)

        // then
        assertThat(result.isEmpty).isTrue()
    }

    private fun createActiveTermVersion(definition: TermsDefinitionJpaEntity): TermsVersionJpaEntity =
        TermsVersionJpaEntity(
            terms = definition,
            version = "1.0",
            termVersionContent = "활성 약관 내용",
            isActive = true,
            effectiveDate = LocalDateTime.now()
        )
}
