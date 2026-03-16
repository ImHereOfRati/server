package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.UserErrorCode
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRelationshipsJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRelationshipsRepository
import com.kdongsu5509.user.adapter.out.persistence.friends.mapper.FriendRelationshipMapper
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.application.port.out.friend.FriendRelationshipSavePort
import com.kdongsu5509.user.domain.friend.FriendRelationship
import com.kdongsu5509.user.domain.friend.FriendRequestUserInfo
import org.springframework.stereotype.Component

@Component
class FriendRelationshipSavePersistenceAdapter(
    private val friendRelationshipMapper: FriendRelationshipMapper,
    private val userRepository: SpringQueryDSLUserRepository,
    private val springDataFriendRelationshipsRepository: SpringDataFriendRelationshipsRepository,
) : FriendRelationshipSavePort {
    override fun save(
        requester: FriendRequestUserInfo,
        receiver: FriendRequestUserInfo
    ): FriendRelationship {
        val newFriendRelationshipEntities = createNewFriendRelationshipEntities(requester.email, receiver.email)
        val result = springDataFriendRelationshipsRepository.saveAll(
            newFriendRelationshipEntities
        )

        return friendRelationshipMapper.mapToDomainEntity(
            result.first {
                it.ownerUser.email == receiver.email
            }
        )
    }

    private fun createNewFriendRelationshipEntities(
        requesterEmail: String,
        receiverEmail: String,
    ): List<FriendRelationshipsJpaEntity> {
        val users = fetchRequiredUsers(requesterEmail, receiverEmail)

        val requester = users.first { it.email == requesterEmail }
        val receiver = users.first { it.email == receiverEmail }

        return buildBidirectionalRelationships(requester, receiver)
    }

    private fun fetchRequiredUsers(vararg emails: String): List<UserJpaEntity> {
        val users = userRepository.findActiveUsersByEmails(emails[0], emails[1])
        if (users.size != emails.size) {
            throw BusinessException(UserErrorCode.USER_NOT_FOUND)
        }
        return users
    }

    private fun buildBidirectionalRelationships(
        requester: UserJpaEntity,
        receiver: UserJpaEntity
    ): List<FriendRelationshipsJpaEntity> {
        return listOf(
            FriendRelationshipsJpaEntity.create(owner = requester, friend = receiver),
            FriendRelationshipsJpaEntity.create(owner = receiver, friend = requester)
        )
    }
}