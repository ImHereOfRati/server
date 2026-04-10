package com.kdongsu5509.user.adapter.out.persistence.friends.jpa

import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface SpringDataFriendRelationshipsRepository : JpaRepository<FriendRelationshipsJpaEntity, UUID> {
    @Query("select f from FriendRelationshipsJpaEntity f join fetch f.ownerUser join fetch f.friendUser where f.ownerUser.id = :ownerUserId")
    fun findByOwnerUserId(ownerUserId: UUID): List<FriendRelationshipsJpaEntity>

    @Query("select f from FriendRelationshipsJpaEntity f join fetch f.ownerUser join fetch f.friendUser where f.friendUser.id = :friendUserId")
    fun findByFriendUserId(friendUserId: UUID): List<FriendRelationshipsJpaEntity>

    fun findByOwnerUserAndFriendUser(
        owner: UserJpaEntity,
        friend: UserJpaEntity
    ): Optional<FriendRelationshipsJpaEntity>

    fun deleteByOwnerUserAndFriendUser(owner: UserJpaEntity, friend: UserJpaEntity)
}
