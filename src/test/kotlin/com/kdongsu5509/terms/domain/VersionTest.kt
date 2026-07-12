package com.kdongsu5509.terms.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class VersionTest {

    @Test
    @DisplayName("first는 첫 버전(1)이다")
    fun first_is_one() {
        assertThat(Version.first().value).isEqualTo(1L)
    }

    @Test
    @DisplayName("next는 값이 1 증가한 새 버전을 만든다")
    fun next_increments() {
        val v = Version.of(3L)

        val next = v.next()

        assertThat(next.value).isEqualTo(4L)
        assertThat(v.value).isEqualTo(3L) // 원본 불변
    }

    @Test
    @DisplayName("같은 값이면 동등하다")
    fun equality_by_value() {
        assertThat(Version.of(2L)).isEqualTo(Version.of(2L))
        assertThat(Version.of(2L)).isNotEqualTo(Version.of(3L))
    }
}
