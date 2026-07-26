package com.kdongsu5509.user.repository.jpa

import com.kdongsu5509.user.domain.OAuth2Provider
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserRole
import com.kdongsu5509.user.domain.UserStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.UUID

class UserJpaEntityTest {

    @Test
    @DisplayName("도메인 사용자로 엔티티를 갱신하면 닉네임과 상태와 Refresh Token 버전만 변경한다")
    fun update_changes_only_mutable_persistence_fields() {
        val entity = UserJpaEntity(
            email = "original@example.com",
            nickname = "기존닉네임",
            role = UserRole.NORMAL,
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.ACTIVE,
            oidcSubject = "original-subject",
            refreshTokenVersion = 2,
        ).apply {
            id = UUID.randomUUID()
        }
        val originalId = entity.id
        val updatedUser = User(
            id = originalId,
            email = "changed@example.com",
            nickname = "변경닉네임",
            role = UserRole.ADMIN,
            oauthProvider = OAuth2Provider.GOOGLE,
            status = UserStatus.BLOCKED,
            oidcSubject = "changed-subject",
            refreshTokenVersion = 3,
        )

        entity.update(updatedUser)

        assertThat(entity.id).isEqualTo(originalId)
        assertThat(entity.email).isEqualTo("original@example.com")
        assertThat(entity.nickname).isEqualTo("변경닉네임")
        assertThat(entity.role).isEqualTo(UserRole.NORMAL)
        assertThat(entity.provider).isEqualTo(OAuth2Provider.KAKAO)
        assertThat(entity.status).isEqualTo(UserStatus.BLOCKED)
        assertThat(entity.oidcSubject).isEqualTo("original-subject")
        assertThat(entity.refreshTokenVersion).isEqualTo(3)
    }

    @Test
    @DisplayName("사용자 엔티티는 기본 역할을 NORMAL로 생성한다")
    fun constructor_uses_normal_role_by_default() {
        val entity = UserJpaEntity(
            email = "default-role@example.com",
            nickname = "기본역할",
            provider = OAuth2Provider.KAKAO,
            status = UserStatus.PENDING,
        )

        assertThat(entity.role).isEqualTo(UserRole.NORMAL)
        assertThat(entity.refreshTokenVersion).isZero()
        assertThat(entity.id).isNull()
    }
}
