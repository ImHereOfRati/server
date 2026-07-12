package com.kdongsu5509.friends.repository.jpa

import com.kdongsu5509.friends.domain.FriendRestrictionType
import com.kdongsu5509.shared.BaseTimeEntity
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "friend_restrictions")
class FriendRestrictionJpaEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restrictor_id")
    val restrictor: UserJpaEntity, // 제한한 사람

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restricted_id")
    val restricted: UserJpaEntity, // 제한 당한 사람

    @field:Enumerated(EnumType.STRING)
    val type: FriendRestrictionType,

    @Column(name = "expired_at")
    val expiredAt: LocalDateTime? = null
) : BaseTimeEntity() {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "friend_restriction_id")
    var id: UUID? = null

    companion object {
        fun create(
            restrictor: UserJpaEntity,
            restricted: UserJpaEntity,
            type: FriendRestrictionType,
            expiredAt: LocalDateTime? = null
        ): FriendRestrictionJpaEntity {
            return FriendRestrictionJpaEntity(
                restrictor = restrictor,
                restricted = restricted,
                type = type,
                expiredAt = expiredAt
            )
        }
    }
}
