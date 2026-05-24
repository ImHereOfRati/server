package com.kdongsu5509.friends.repository.jpa

import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SpringDataFriendshipRepository : JpaRepository<FriendshipJpaEntity, UUID> {
    fun findByOwnerUserEmail(email: String, pageable: Pageable): Slice<FriendshipJpaEntity>

    fun findByOwnerUserEmailAndFriendUserEmail(ownerEmail: String, friendEmail: String): Optional<FriendshipJpaEntity>

    fun findByOwnerUserEmailAndFriendUserId(ownerEmail: String, friendId: UUID): Optional<FriendshipJpaEntity>

    fun deleteByOwnerUserEmailAndFriendUserEmail(ownerEmail: String, friendEmail: String)

    @Modifying(clearAutomatically = true)
    @Query(
        """
    DELETE FROM FriendshipJpaEntity f 
    WHERE (f.ownerUser.id = :ownerId AND f.friendUser.id = :friendId) 
       OR (f.ownerUser.id = :friendId AND f.friendUser.id = :ownerId)
"""
    )
    fun deleteBoth(@Param("ownerId") ownerId: UUID, @Param("friendId") friendId: UUID)

    fun existsByOwnerUserIdAndFriendUserId(ownerId: UUID, friendId: UUID): Boolean
}
