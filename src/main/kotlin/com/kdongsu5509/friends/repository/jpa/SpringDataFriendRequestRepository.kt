package com.kdongsu5509.friends.repository.jpa

import com.kdongsu5509.user.repository.jpa.UserJpaEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SpringDataFriendRequestRepository : JpaRepository<FriendRequestJpaEntity, UUID> {
    fun findAllByReceiverEmail(email: String, pageable: Pageable): Slice<FriendRequestJpaEntity>
    fun findAllByRequesterEmail(email: String, pageable: Pageable): Slice<FriendRequestJpaEntity>
    fun deleteByRequesterAndReceiver(requester: UserJpaEntity, receiver: UserJpaEntity)
    fun existsByRequesterIdAndReceiverId(requesterId: UUID, receiverId: UUID): Boolean
}
