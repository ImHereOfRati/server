package com.kdongsu5509.terms.repository

import com.kdongsu5509.terms.domain.TermTypes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
class SpringDataTermRepositoryTest @Autowired constructor(
    private val termRepository: SpringDataTermRepository
) {

    @Test
    @DisplayName("타입별로 현재 시점 이전에 적용된 가장 최근 버전의 약관들을 조회한다")
    fun findActiveAll_success() {
        // given
        val now = LocalDateTime.now()

        // SERVICE 약관 2개 (과거 버전, 현재 버전)
        val serviceV1 = termRepository.save(createEntity(TermTypes.SERVICE, "v1", 1L, now.minusDays(10)))
        val serviceV2 = termRepository.save(createEntity(TermTypes.SERVICE, "v2", 2L, now.minusDays(1)))

        // PRIVACY 약관 2개 (현재 버전, 미래 버전)
        val privacyV1 = termRepository.save(createEntity(TermTypes.PRIVACY, "v1", 1L, now.minusDays(5)))
        val privacyV2 = termRepository.save(createEntity(TermTypes.PRIVACY, "v2", 2L, now.plusDays(1)))

        // when
        val activeTerms = termRepository.findActiveAll()

        // then
        assertThat(activeTerms).hasSize(2)

        val serviceTerm = activeTerms.find { it.type == TermTypes.SERVICE }
        assertThat(serviceTerm?.id).isEqualTo(serviceV2.id)
        assertThat(serviceTerm?.title).isEqualTo("v2")

        val privacyTerm = activeTerms.find { it.type == TermTypes.PRIVACY }
        assertThat(privacyTerm?.id).isEqualTo(privacyV1.id)
        assertThat(privacyTerm?.title).isEqualTo("v1")
    }

    private fun createEntity(
        type: TermTypes,
        title: String,
        version: Long,
        effectiveDate: LocalDateTime
    ) = TermJpaEntity(
        version = version,
        type = type,
        title = title,
        content = "내용",
        effectiveDate = effectiveDate,
        isRequired = true
    )
}
