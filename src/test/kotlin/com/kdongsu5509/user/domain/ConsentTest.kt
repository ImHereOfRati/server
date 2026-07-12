package com.kdongsu5509.user.domain

import com.kdongsu5509.support.exception.ImHereBaseException
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.service.TermResult
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ConsentTest {

    private fun term(id: Long, type: TermTypes, isRequired: Boolean) =
        TermResult(id, 1L, type, "제목", "내용", LocalDateTime.now(), isRequired)

    @Test
    @DisplayName("제출한 동의가 모두 유효 약관에 포함되면 검증을 통과한다")
    fun validateAgainst_success() {
        val activeTerms = listOf(term(1L, TermTypes.SERVICE, true), term(2L, TermTypes.MARKETING, false))
        val consent = Consent(listOf(Consent.Item(1L, true), Consent.Item(2L, false)))

        assertThatCode { consent.validateAgainst(activeTerms) }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("유효 약관에 없는 약관에 동의를 제출하면 TERM_NOT_FOUND 예외가 발생한다")
    fun validateAgainst_fail_term_not_found() {
        val activeTerms = listOf(term(1L, TermTypes.SERVICE, true))
        val consent = Consent(listOf(Consent.Item(1L, true), Consent.Item(999L, true)))

        assertThatThrownBy { consent.validateAgainst(activeTerms) }
            .isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(TermException.TERM_NOT_FOUND)
    }

    @Test
    @DisplayName("필수 약관에 모두 동의하면 검증을 통과한다")
    fun assertRequiredAgreed_success() {
        val activeTerms = listOf(term(1L, TermTypes.SERVICE, true), term(2L, TermTypes.MARKETING, false))
        val consent = Consent(listOf(Consent.Item(1L, true), Consent.Item(2L, false)))

        assertThatCode { consent.assertRequiredAgreed(activeTerms) }.doesNotThrowAnyException()
    }

    @Test
    @DisplayName("필수 약관 중 하나라도 동의하지 않으면 OBLIGATORY_TERM_NOT_AGREED 예외가 발생한다")
    fun assertRequiredAgreed_fail() {
        val activeTerms = listOf(term(1L, TermTypes.SERVICE, true), term(2L, TermTypes.PRIVACY, true))
        val consent = Consent(listOf(Consent.Item(1L, true), Consent.Item(2L, false)))

        assertThatThrownBy { consent.assertRequiredAgreed(activeTerms) }
            .isInstanceOf(ImHereBaseException::class.java)
            .extracting("errorCode")
            .isEqualTo(TermException.OBLIGATORY_TERM_NOT_AGREED)
    }

    @Test
    @DisplayName("agreedTermIds는 동의(isAgreed=true)한 약관 ID만 순서대로 반환한다")
    fun agreedTermIds_returns_only_agreed() {
        val consent = Consent(
            listOf(Consent.Item(1L, true), Consent.Item(2L, false), Consent.Item(3L, true))
        )

        assertThat(consent.agreedTermIds()).containsExactly(1L, 3L)
    }
}
