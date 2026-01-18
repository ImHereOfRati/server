package com.kdongsu5509.imhereuserservice.adapter.out.persistence

import com.kdongsu5509.imhereuserservice.domain.FriendshipStatus
import jakarta.persistence.*
import org.hibernate.annotations.UuidGenerator
import java.util.*

@Entity
@Table(name = "friendship")
class FriendshipJpaEntity : BaseTimeEntity {
    @Id
    @GeneratedValue
    @UuidGenerator
    var id: UUID? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "register_id")
    var register: UserJpaEntity? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id")
    var receiver: UserJpaEntity? = null

    @Enumerated(EnumType.STRING)
    var friendshipStatus: FriendshipStatus? = null

    constructor(
        register: UserJpaEntity?,
        receiver: UserJpaEntity?,
        friendshipStatus: FriendshipStatus?
    ) : this() {
        this.register = register
        this.receiver = receiver
        this.friendshipStatus = friendshipStatus
    }

    protected constructor()
}