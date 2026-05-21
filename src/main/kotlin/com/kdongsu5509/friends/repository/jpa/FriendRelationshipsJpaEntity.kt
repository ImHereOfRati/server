package com.kdongsu5509.friends.repository.jpa

import com.kdongsu5509.shared.BaseTimeEntity
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.util.*

@Entity
@Table(
    name = "friend_relationships",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_owner_friend", columnNames = ["owner_user_id", "friend_user_id"])
    ]
)
class FriendRelationshipsJpaEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    val ownerUser: UserJpaEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_user_id", nullable = false)
    val friendUser: UserJpaEntity,

    @Column(name = "friend_alias", nullable = false, length = 20)
    var friendAlias: String
) : BaseTimeEntity() {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "friend_relationship_id")
    val id: UUID? = null

    init {
        require(ownerUser.id != friendUser.id) { "자기 자신을 친구로 등록할 수 없습니다." }
    }

    fun updateAlias(newAlias: String) {
        require(newAlias.isNotBlank()) { "별칭은 공백일 수 없습니다." }
        this.friendAlias = newAlias
    }

    companion object {
        fun create(
            owner: UserJpaEntity,
            friend: UserJpaEntity,
            alias: String? = null
        ): FriendRelationshipsJpaEntity {
            return FriendRelationshipsJpaEntity(
                ownerUser = owner,
                friendUser = friend,
                friendAlias = alias ?: friend.nickname
            )
        }
    }
}
