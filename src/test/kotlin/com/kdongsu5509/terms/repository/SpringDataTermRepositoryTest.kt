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
    @DisplayName("타입별 최고 버전의 약관을 조회한다")
    fun findTopByTypeOrderByVersionDesc_success() {
        termRepository.save(createEntity(TermTypes.SERVICE, 1L, LocalDateTime.now().minusDays(2)))
        val latest = termRepository.save(createEntity(TermTypes.SERVICE, 2L, LocalDateTime.now().plusDays(1)))

        val result = termRepository.findTopByTypeOrderByVersionDesc(TermTypes.SERVICE)

        assertThat(result?.id).isEqualTo(latest.id)
        assertThat(result?.version).isEqualTo(2L)
    }

    @Test
    @DisplayName("지정한 시점까지 시행된 모든 약관을 조회한다")
    fun findAllByEffectiveDateLessThanEqual_success() {
        val now = LocalDateTime.now()
        termRepository.save(createEntity(TermTypes.SERVICE, 1L, now.minusDays(2)))
        termRepository.save(createEntity(TermTypes.SERVICE, 2L, now.minusDays(1)))
        termRepository.save(createEntity(TermTypes.PRIVACY, 1L, now.plusDays(1)))

        val results = termRepository.findAllByEffectiveDateLessThanEqual(now)

        assertThat(results).hasSize(2)
        assertThat(results).allMatch { !it.effectiveDate.isAfter(now) }
    }

    private fun createEntity(
        type: TermTypes,
        version: Long,
        effectiveDate: LocalDateTime,
    ) = TermJpaEntity(
        version = version,
        type = type,
        title = "v$version",
        content = "내용",
        effectiveDate = effectiveDate,
        isRequired = true,
    )
}
