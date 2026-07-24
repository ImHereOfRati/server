package com.kdongsu5509.agreement.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class ConsentTest {

    @Test
    @DisplayName("현재 상태와 실제로 달라진 동의 상태만 변경 내역으로 반환한다")
    fun changes_success_returns_only_actual_state_transitions() {
        // given
        val consent = Consent(
            listOf(
                ConsentItem(1L, true),
                ConsentItem(2L, true),
                ConsentItem(3L, false),
                ConsentItem(4L, false),
            )
        )
        val newStatus = mapOf(
            2L to AgreementStatus.CONSENT,
            3L to AgreementStatus.CONSENT,
            4L to AgreementStatus.WITHDRAW,
        )

        // when
        val consentChanges = consent.changes(newStatus)

        // then
        assertThat(consentChanges).containsExactly(
            ConsentChange(1L, AgreementStatus.CONSENT),
            ConsentChange(3L, AgreementStatus.WITHDRAW),
        )
    }

    @Test
    @DisplayName("같은 약관의 동의 요청이 중복되면 마지막 요청을 적용한다")
    fun changes_success_applies_last_duplicate_consent_request() {
        // given
        val consent = Consent(listOf(ConsentItem(1L, false), ConsentItem(1L, true)))

        // when
        val consentChanges = consent.changes(emptyMap())

        // then
        assertThat(consentChanges).containsExactly(
            ConsentChange(1L, AgreementStatus.CONSENT)
        )
    }
}
