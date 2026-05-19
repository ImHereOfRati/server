package com.kdongsu5509.terms.adapter.out.jpa

import com.kdongsu5509.terms.domain.TermsTypes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
class SpringDataTermPolicyRepositoryTest @Autowired constructor(
    private val repository: SpringDataTermPolicyRepository
) {
    @Test
    @DisplayName("약관 제목과 타입이 일치하는 데이터가 있으면 true를 반환한다")
    fun existsByTitleAndType_success() {
        // given
        val title = "서비스 이용약관"
        val type = TermsTypes.SERVICE
        val entity = TermPolicyJpaEntity(title, type, true)
        repository.save(entity)

        // when
        val result = repository.existsByTitleAndType(title, type)

        // then
        assertThat(result).isTrue()
    }

    @Test
    @DisplayName("일치하는 약관 제목이 없으면 false를 반환한다")
    fun existsByTermsTitleAndType_fail_when_title_mismatch() {
        // given
        val title = "존재하지 않는 제목"
        val type = TermsTypes.SERVICE

        val termPolicyJpaEntity = TermPolicyJpaEntity("실제 저장된 제목", type, true)
        repository.save(termPolicyJpaEntity)

        // when
        val result = repository.existsByTitleAndType(title, type)

        // then
        assertThat(result).isFalse()
    }

    @Test
    @DisplayName("일치하는 약관 타입이 없으면 false를 반환한다")
    fun existsByTitleAndType_fail_when_type_mismatch() {
        // given
        val title = "서비스 이용약관"
        val type = TermsTypes.SERVICE

        val termPolicyJpaEntity = TermPolicyJpaEntity(title, TermsTypes.LOCATION, true)
        repository.save(termPolicyJpaEntity)

        // when
        val result = repository.existsByTitleAndType(title, type)

        // then
        assertThat(result).isFalse()
    }
}
