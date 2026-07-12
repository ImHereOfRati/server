package com.kdongsu5509.user.domain

import com.kdongsu5509.support.exception.throwIt
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.service.TermResult

/**
 * 사용자가 제출한 약관 동의 묶음을 표현하는 값객체.
 * "유효 약관 포함 여부", "필수 약관 충족 여부", "동의한 약관 ID 산출"이라는
 * 약관 동의 불변식을 스스로 소유한다(P2 / 진단 C1).
 * 서비스는 조회→위임→저장→활성화 조율만 담당한다.
 */
data class Consent(
    val items: List<Item>,
) {
    data class Item(
        val termId: Long,
        val isAgreed: Boolean,
    )

    /** 제출한 모든 동의가 유효(활성) 약관 집합에 포함되는지 검증한다. */
    fun validateAgainst(activeTerms: List<TermResult>): Consent {
        val activeTermIds = activeTerms.map { it.id }.toSet()
        if (items.any { it.termId !in activeTermIds }) {
            TermException.TERM_NOT_FOUND.throwIt()
        }
        return this
    }

    /** 필수 약관이 모두 동의되었는지 검증한다. */
    fun assertRequiredAgreed(activeTerms: List<TermResult>): Consent {
        val agreedMap = items.associate { it.termId to it.isAgreed }
        val allRequiredAgreed = activeTerms.filter { it.isRequired }.all { agreedMap[it.id] == true }
        if (!allRequiredAgreed) {
            TermException.OBLIGATORY_TERM_NOT_AGREED.throwIt()
        }
        return this
    }

    /** 실제로 동의(isAgreed=true)한 약관 ID 목록을 제출 순서대로 반환한다. */
    fun agreedTermIds(): List<Long> = items.filter { it.isAgreed }.map { it.termId }
}
