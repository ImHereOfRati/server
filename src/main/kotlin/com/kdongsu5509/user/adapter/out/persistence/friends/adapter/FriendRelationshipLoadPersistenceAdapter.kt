package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.FriendErrorCode
import com.kdongsu5509.support.exception.UserErrorCode
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRelationshipsJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRelationshipsRepository
import com.kdongsu5509.user.adapter.out.persistence.friends.mapper.FriendRelationshipMapper
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.application.port.out.friend.FriendRelationshipLoadPort
import com.kdongsu5509.user.domain.friend.FriendRelationship
import org.springframework.stereotype.Component
import java.util.*

@Component
class FriendRelationshipLoadPersistenceAdapter(
    private val friendRelationshipMapper: FriendRelationshipMapper,
    private val userRepository: SpringQueryDSLUserRepository,
    private val springDataFriendRelationshipsRepository: SpringDataFriendRelationshipsRepository,
) : FriendRelationshipLoadPort {
    override fun findFriendsRelationshipsByUserEmail(email: String): List<FriendRelationship> {
        val userEntity = fetchRequiredUser(email)
        val relationshipEntities = springDataFriendRelationshipsRepository.findByOwnerUserId(userEntity.id!!)
        return relationshipEntities.map { friendRelationshipMapper.mapToDomainEntity(it) }
    }

    override fun findFriendRelationshipByRelationshipId(id: UUID): FriendRelationship {
        return friendRelationshipMapper.mapToDomainEntity(
            fetchRequiredFriendRelationship(id)
        )
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