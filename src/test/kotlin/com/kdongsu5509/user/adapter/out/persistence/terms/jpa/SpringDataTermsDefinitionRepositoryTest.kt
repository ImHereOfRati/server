package com.kdongsu5509.user.adapter.out.persistence.terms.jpa

import com.kdongsu5509.user.domain.terms.TermsTypes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@DataJpaTest
class SpringDataTermsDefinitionRepositoryTest @Autowired constructor(
    private val repository: SpringDataTermsDefinitionRepository
) {
    @Test
    @DisplayName("약관 제목과 타입이 일치하는 데이터가 있으면 true를 반환한다.")
    fun existsByTermsTitleAndTermsType_ReturnTrue() {
        // given
        val title = "서비스 이용약관"
        val type = TermsTypes.SERVICE
        val entity = TermsDefinitionJpaEntity(title, type, true)
        repository.save(entity)

        // when
        val result = repository.existsByTermsTitleAndTermsType(title, type)

        // then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("일치하는 약관 제목이 없으면 false를 반환한다.")
    fun existsByTermsTitleAndTermsType_ReturnFalse_WhenTitleMismatch() {
        // given
        val title = "존재하지 않는 약관"
        val type = TermsTypes.SERVICE
        repository.save(TermsDefinitionJpaEntity("실제 저장된 약관", type, true))

        // when
        val result = repository.existsByTermsTitleAndTermsType(title, type)

        // then
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("일치하는 약관 타입이 없으면 false를 반환한다.")
    fun existsByTermsTitleAndTermsType_ReturnFalse_WhenTypeMismatch() {
        // given
        val title = "서비스 이용약관"
        val type = TermsTypes.SERVICE
        repository.save(TermsDefinitionJpaEntity(title, TermsTypes.LOCATION, true))

        // when
        val result = repository.existsByTermsTitleAndTermsType(title, type)

        // then
        assertThat(result).isFalse()
    }
}
