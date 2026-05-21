package com.kdongsu5509.friends.repository.jpa

import com.kdongsu5509.shared.BaseTimeEntity
import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.util.*

@Entity
@Table(name = "friend_request")
class FriendRequestJpaEntity(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    val requester: UserJpaEntity,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    val receiver: UserJpaEntity,

    @Column(nullable = false)
    val message: String
) : BaseTimeEntity() {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "friend_request_id")
    var id: UUID? = null
}
