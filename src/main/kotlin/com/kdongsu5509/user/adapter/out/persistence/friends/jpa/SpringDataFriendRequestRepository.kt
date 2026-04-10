package com.kdongsu5509.user.adapter.out.persistence.friends.jpa

import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SpringDataFriendRequestRepository : JpaRepository<FriendRequestJpaEntity, Long> {
    fun findByReceiverEmail(receiverEmail: String): List<FriendRequestJpaEntity>
    fun deleteByRequesterAndReceiver(requester: UserJpaEntity, receiver: UserJpaEntity)
}