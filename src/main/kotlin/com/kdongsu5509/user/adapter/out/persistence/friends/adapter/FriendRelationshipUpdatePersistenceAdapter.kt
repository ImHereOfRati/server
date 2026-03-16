package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.FriendErrorCode
import com.kdongsu5509.support.exception.UserErrorCode
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRelationshipsJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRelationshipsRepository
import com.kdongsu5509.user.adapter.out.persistence.friends.mapper.FriendRelationshipMapper
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.application.port.out.friend.FriendRelationshipUpdatePort
import com.kdongsu5509.user.domain.friend.FriendRelationship
import org.springframework.stereotype.Component
import java.util.*

@Component
class FriendRelationshipUpdatePersistenceAdapter(
    private val friendRelationshipMapper: FriendRelationshipMapper,
    private val userRepository: SpringQueryDSLUserRepository,
    private val springDataFriendRelationshipsRepository: SpringDataFriendRelationshipsRepository,
) : FriendRelationshipUpdatePort {
    override fun updateAlias(
        userEmail: String,
        friendRelationshipId: UUID,
        newFriendAlias: String
    ): FriendRelationship {
        val friendRelationshipEntity = fetchRequiredFriendRelationship(friendRelationshipId)
        if (friendRelationshipEntity.ownerUser.email != userEmail) {
            throw BusinessException(FriendErrorCode.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH)
        }

        friendRelationshipEntity.updateAlias(newFriendAlias)
        val updatedEntity = springDataFriendRelationshipsRepository.save(friendRelationshipEntity)
        return friendRelationshipMapper.mapToDomainEntity(updatedEntity)
    }

    override fun delete(userEmail: String, friendRelationshipId: UUID) {
        val friendRelationshipEntity = fetchRequiredFriendRelationship(friendRelationshipId)

        if (friendRelationshipEntity.ownerUser.email != userEmail) {
            throw BusinessException(FriendErrorCode.FRIEND_RELATIONSHIP_OWNER_MISS_MATCH)
        }

        val otherRelationship = springDataFriendRelationshipsRepository.findByOwnerUserAndFriendUser(
            owner = friendRelationshipEntity.friendUser,
            friend = friendRelationshipEntity.ownerUser
        ).orElseThrow { BusinessException(FriendErrorCode.FRIEND_RELATIONSHIP_NOT_FOUND) }

        springDataFriendRelationshipsRepository.delete(friendRelationshipEntity)
        springDataFriendRelationshipsRepository.delete(otherRelationship)
    }

    private fun fetchRequiredUser(email: String): UserJpaEntity {
        return userRepository.findUserByEmail(email).orElseThrow {
            BusinessException(UserErrorCode.USER_NOT_FOUND)
        }
    }

    private fun fetchRequiredFriendRelationship(id: UUID): FriendRelationshipsJpaEntity =
        springDataFriendRelationshipsRepository.findById(id).orElseThrow {
            throw BusinessException(FriendErrorCode.FRIEND_RELATIONSHIP_NOT_FOUND)
        }
}