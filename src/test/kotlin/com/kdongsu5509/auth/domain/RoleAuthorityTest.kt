package com.kdongsu5509.auth.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class RoleAuthorityTest {

    @Test
    @DisplayName("role에 ROLE_ 접두를 부착한다")
    fun toAuthority_prefixes() {
        assertThat(RoleAuthority.toAuthority("NORMAL")).isEqualTo("ROLE_NORMAL")
        assertThat(RoleAuthority.toAuthority("ADMIN")).isEqualTo("ROLE_ADMIN")
    }

    @Test
    @DisplayName("ROLE_ 접두를 제거해 원래 role을 복원한다")
    fun fromAuthority_strips() {
        assertThat(RoleAuthority.fromAuthority("ROLE_NORMAL")).isEqualTo("NORMAL")
        assertThat(RoleAuthority.fromAuthority("ROLE_ADMIN")).isEqualTo("ADMIN")
    }

    @Test
    @DisplayName("부착 후 제거하면 원래 값으로 왕복한다")
    fun roundtrip() {
        val role = "NORMAL"
        assertThat(RoleAuthority.fromAuthority(RoleAuthority.toAuthority(role))).isEqualTo(role)
    }

    @Test
    @DisplayName("접두가 없는 값은 제거해도 그대로다")
    fun fromAuthority_noPrefix_unchanged() {
        assertThat(RoleAuthority.fromAuthority("NORMAL")).isEqualTo("NORMAL")
    }
}
