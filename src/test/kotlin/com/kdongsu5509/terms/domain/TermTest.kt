package com.kdongsu5509.terms.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TermTest {

    @Nested
    @DisplayName("isEffectiveAt: 특정 시점 기준 발효 여부")
    inner class IsEffectiveAt {

        @Test
        @DisplayName("발효일이 기준 시점보다 과거면 발효되어 있다")
        fun past_effectiveDate_is_effective() {
            val now = LocalDateTime.now()
            val term = sampleTerm(effectiveDate = now.minusSeconds(1))

            assertThat(term.isEffectiveAt(now)).isTrue()
        }

        @Test
        @DisplayName("발효일이 기준 시점과 같으면 발효되어 있다(경계값 포함)")
        fun equal_effectiveDate_is_effective() {
            val now = LocalDateTime.now()
            val term = sampleTerm(effectiveDate = now)

            assertThat(term.isEffectiveAt(now)).isTrue()
        }

        @Test
        @DisplayName("발효일이 기준 시점보다 미래면 아직 발효되지 않았다")
        fun future_effectiveDate_is_not_effective() {
            val now = LocalDateTime.now()
            val term = sampleTerm(effectiveDate = now.plusSeconds(1))

            assertThat(term.isEffectiveAt(now)).isFalse()
        }
    }

    @Nested
    @DisplayName("issueNext: 최신 약관 기준 다음 버전 채번")
    inner class IssueNext {

        @Test
        @DisplayName("이전 약관이 없으면 첫 버전(1)으로 발행한다")
        fun without_previous_issues_first_version() {
            val issued = Term.issueNext(
                previous = null,
                type = TermTypes.SERVICE,
                title = "제목",
                content = "내용",
                effectiveDate = LocalDateTime.now(),
                isRequired = true,
            )

            assertThat(issued.version.value).isEqualTo(1L)
            assertThat(issued.id).isNull()
            assertThat(issued.type).isEqualTo(TermTypes.SERVICE)
        }

        @Test
        @DisplayName("이전 약관이 있으면 그 버전 +1로 발행한다")
        fun with_previous_issues_incremented_version() {
            val previous = sampleTerm(version = 3L)

            val issued = Term.issueNext(
                previous = previous,
                type = TermTypes.SERVICE,
                title = "제목",
                content = "내용",
                effectiveDate = LocalDateTime.now(),
                isRequired = true,
            )

            assertThat(issued.version.value).isEqualTo(4L)
        }
    }

    private fun sampleTerm(
        version: Long = 1L,
        effectiveDate: LocalDateTime = LocalDateTime.now(),
    ) = Term.reconstruct(
        id = 1L,
        version = version,
        type = TermTypes.SERVICE,
        title = "제목",
        content = "내용",
        effectiveDate = effectiveDate,
        isRequired = true,
    )
}
