package com.kdongsu5509.friends.repository.jpa

import com.kdongsu5509.shared.BaseTimeEntity
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.util.*

/**
 * 친구 요청을 수락하면 `friend_relationships` 테이블에 양방향 관계를 위해
 * 두 개의 행이 저장됩니다.
 *   - 첫 번째 행: ownerUser (요청자) → friendUser (수신자)
 *   - 두 번째 행: ownerUser (수신자) → friendUser (요청자)
 * 이 설계는 양쪽 사용자가 각각 자신의 친구 목록을 조회할 때
 * 별도의 조회 없이 바로 관계를 확인할 수 있도록 합니다.
 */

@Entity
@Table(
    name = "friend_relationships",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_owner_friend", columnNames = ["owner_user_id", "friend_user_id"])
    ]
)
class FriendshipJpaEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    val ownerUser: UserJpaEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friend_user_id", nullable = false)
    val friendUser: UserJpaEntity,

    @Column(name = "friend_alias", nullable = false, length = 20)
    var friendAlias: String
) : BaseTimeEntity() {

    init {
        require(ownerUser.id != friendUser.id) { "자기 자신을 친구로 등록할 수 없습니다." }
    }

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "friend_relationship_id")
    val id: UUID? = null

    companion object {
        fun create(
            owner: UserJpaEntity,
            friend: UserJpaEntity,
            alias: String
        ): FriendshipJpaEntity {
            return FriendshipJpaEntity(
                ownerUser = owner,
                friendUser = friend,
                friendAlias = alias
            )
        }
    }
}
