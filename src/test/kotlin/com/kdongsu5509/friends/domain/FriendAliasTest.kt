package com.kdongsu5509.friends.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FriendAliasTest {

    @Test
    @DisplayName("20자 이하면 정상 생성된다")
    fun success() {
        val alias = FriendAlias("a".repeat(20))

        assertThat(alias.value).isEqualTo("a".repeat(20))
    }

    @Test
    @DisplayName("20자를 넘으면 예외를 발생시킨다")
    fun tooLong_throws() {
        assertThrows<IllegalArgumentException> {
            FriendAlias("a".repeat(21))
        }
    }

    @Test
    @DisplayName("fromNickname은 20자를 넘는 닉네임을 잘라서 생성한다")
    fun fromNickname_truncatesLongNickname() {
        val alias = FriendAlias.fromNickname("a".repeat(30))

        assertThat(alias.value).isEqualTo("a".repeat(20))
    }

    @Test
    @DisplayName("fromNickname은 20자 이하 닉네임을 그대로 사용한다")
    fun fromNickname_keepsShortNickname() {
        val alias = FriendAlias.fromNickname("short")

        assertThat(alias.value).isEqualTo("short")
    }
}
