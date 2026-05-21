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
        fun createRejectionType(
            actor: UserJpaEntity,
            target: UserJpaEntity
        ): FriendRestrictionJpaEntity {
            return FriendRestrictionJpaEntity(
                restrictor = actor,
                restricted = target,
                type = FriendRestrictionType.REJECT,
                expiredAt = LocalDateTime.now().plusDays(30)
            )
        }

        fun create(
            actor: UserJpaEntity,
            target: UserJpaEntity,
            type: FriendRestrictionType
        ): FriendRestrictionJpaEntity {
            val expiredAt = if (type == FriendRestrictionType.REJECT) {
                LocalDateTime.now().plusDays(30)
            } else {
                null
            }
            return FriendRestrictionJpaEntity(
                restrictor = actor,
                restricted = target,
                type = type,
                expiredAt = expiredAt
            )
        }
    }
}
