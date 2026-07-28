package com.kdongsu5509.friends.repository.jpa

import com.kdongsu5509.friends.domain.FriendRelationStatus
import com.kdongsu5509.shared.repository.BaseTimeEntity
import jakarta.persistence.*
import jakarta.persistence.EnumType.STRING
import org.hibernate.annotations.UuidGenerator
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(
    name = "friend_relations",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_friend_pair", columnNames = ["low_user_id", "high_user_id"])
    ],
    indexes = [
        Index(name = "idx_friend_relations_low", columnList = "low_user_id, status"),
        Index(name = "idx_friend_relations_high", columnList = "high_user_id, status"),
        Index(name = "idx_friend_relations_expired_at", columnList = "expired_at")
    ]
)
class FriendRelationJpaEntity(
    @Column(name = "low_user_id", nullable = false)
    val lowUserId: UUID,

    @Column(name = "high_user_id", nullable = false)
    val highUserId: UUID,

    @Enumerated(STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: FriendRelationStatus,

    @Column(name = "initiated_user_id", nullable = false)
    var initiatedUserId: UUID,

    @Column(name = "message", length = 255)
    var message: String? = null,

    @Column(name = "low_alias", length = 10)
    var lowAlias: String? = null,

    @Column(name = "high_alias", length = 10)
    var highAlias: String? = null,

    @Column(name = "expired_at")
    var expiredAt: LocalDateTime? = null
) : BaseTimeEntity() {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "friend_relation_id")
    var id: UUID? = null

    fun apply(
        status: FriendRelationStatus,
        initiatedBy: UUID,
        message: String?,
        lowAlias: String?,
        highAlias: String?,
        expiredAt: LocalDateTime?
    ) {
        this.status = status
        this.initiatedUserId = initiatedBy
        this.message = message
        this.lowAlias = lowAlias
        this.highAlias = highAlias
        this.expiredAt = expiredAt
    }
}
