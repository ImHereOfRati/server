package com.kdongsu5509.user.repository.jpa

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.auth.domain.UserStatus
import com.kdongsu5509.shared.BaseTimeEntity
import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.util.*

@Entity
@Table(
    name = "users",
    indexes = [
        Index(
            name = "idx_users_nickname",
            columnList = "nickname"
        )
    ]
)
class UserJpaEntity(
    @Column(unique = true, nullable = false)
    val email: String,

    @Column(nullable = false)
    var nickname: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: UserRole = UserRole.NORMAL,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val provider: OAuth2Provider,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UserStatus
) : BaseTimeEntity() {
    @Id
    @GeneratedValue
    @UuidGenerator
    var id: UUID? = null

    fun activate() {
        this.status = UserStatus.ACTIVE
    }

    fun block() {
        this.status = UserStatus.BLOCKED
    }

    fun unblock() {
        this.status = UserStatus.ACTIVE
    }

    fun changeNickname(newNickname: String) {
        this.nickname = newNickname
    }
}
