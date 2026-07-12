package com.kdongsu5509.terms.domain

import java.time.LocalDateTime

data class Term private constructor(
    val id: Long?,
    val version: Version,
    val type: TermTypes,
    val title: String,
    val content: String,
    val effectiveDate: LocalDateTime,
    val isRequired: Boolean,
) {
    /**
     * 주어진 시점 기준으로 이 약관이 발효되어 있는지 스스로 판단한다.
     * effectiveDate <= now 이면 발효(경계값 포함).
     */
    fun isEffectiveAt(now: LocalDateTime): Boolean = !effectiveDate.isAfter(now)

    companion object {
        /**
         * 같은 타입의 이전(최신) 약관을 기준으로 다음 버전을 채번해 새 약관을 발행한다.
         * 이전 약관이 없으면 첫 버전, 있으면 그 버전의 next(). 채번 규칙을 도메인이 소유한다.
         * 신규 발행이므로 id는 아직 없다(null).
         */
        fun issueNext(
            previous: Term?,
            type: TermTypes,
            title: String,
            content: String,
            effectiveDate: LocalDateTime,
            isRequired: Boolean,
        ): Term {
            val nextVersion = previous?.version?.next() ?: Version.first()
            return Term(
                id = null,
                version = nextVersion,
                type = type,
                title = title,
                content = content,
                effectiveDate = effectiveDate,
                isRequired = isRequired,
            )
        }

        /**
         * 영속 상태로부터 도메인을 복원하는 팩토리(id 포함). 매퍼 경계에서 사용.
         * 경계는 Long 버전을 넘기고 여기서 Version으로 변환한다.
         */
        fun reconstruct(
            id: Long?,
            version: Long,
            type: TermTypes,
            title: String,
            content: String,
            effectiveDate: LocalDateTime,
            isRequired: Boolean,
        ): Term {
            return Term(
                id = id,
                version = Version.of(version),
                type = type,
                title = title,
                content = content,
                effectiveDate = effectiveDate,
                isRequired = isRequired,
            )
        }
    }
}
