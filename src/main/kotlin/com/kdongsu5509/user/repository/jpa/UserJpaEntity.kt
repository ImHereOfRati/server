package com.kdongsu5509.user.repository.jpa

import com.kdongsu5509.auth.domain.OAuth2Provider
import com.kdongsu5509.auth.domain.UserRole
import com.kdongsu5509.shared.BaseTimeEntity
import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.domain.UserStatus
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

    fun update(user: User) {
        this.nickname = user.nickname
        this.status = user.status
    }
}
