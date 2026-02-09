package com.kdongsu5509.imhereuserservice.adapter.out.persistence.friends.jpa

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SpringDataFriendRequestRepository : JpaRepository<FriendRequestJpaEntity, UUID> {
}