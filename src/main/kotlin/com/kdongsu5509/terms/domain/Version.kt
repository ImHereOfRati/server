package com.kdongsu5509.terms.domain

/**
 * 약관 버전 값 객체. "+1"/"첫 버전" 채번 규칙을 원시 Long이 아닌 타입이 소유한다.
 * 경계(응답 DTO/JPA 엔티티)는 여전히 Long을 쓰며, 매퍼에서 Long↔Version 변환한다.
 */
data class Version(val value: Long) {
    fun next(): Version = Version(value + 1L)

    companion object {
        val FIRST: Version = Version(1L)

        fun first(): Version = FIRST

        fun of(value: Long): Version = Version(value)
    }
}
