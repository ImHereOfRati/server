package com.kdongsu5509.imhereuserservice.adapter.out.persistence.user.jpa

import com.kdongsu5509.imhereuserservice.adapter.out.persistence.common.BaseTimeEntity
import com.kdongsu5509.imhereuserservice.domain.user.OAuth2Provider
import com.kdongsu5509.imhereuserservice.domain.user.UserRole
import com.kdongsu5509.imhereuserservice.domain.user.UserStatus
import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.util.*

@Entity
@Table(name = "users")
class UserJpaEntity(
    @Column(unique = true, nullable = false)
    var email: String,

    @Column(nullable = false)
    var nickname: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: UserRole = UserRole.NORMAL,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var provider: OAuth2Provider,

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

    fun changeNickname(newNickname: String) {
        this.nickname = newNickname
    }
}