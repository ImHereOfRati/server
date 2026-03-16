package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.UserErrorCode
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.FriendRestrictionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRestrictionRepository
import com.kdongsu5509.user.adapter.out.persistence.friends.mapper.FriendRestrictionMapper
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.application.port.out.friend.FriendRestrictionSavePort
import com.kdongsu5509.user.domain.friend.FriendRequestUserInfo
import com.kdongsu5509.user.domain.friend.FriendRestriction
import com.kdongsu5509.user.domain.friend.FriendRestrictionType
import org.springframework.stereotype.Component

@Component
class FriendRestrictionSavePersistenceAdapter(
    private val friendRestrictionMapper: FriendRestrictionMapper,
    private val userRepository: SpringQueryDSLUserRepository,
    private val springDataFriendRestrictionRepository: SpringDataFriendRestrictionRepository,
) : FriendRestrictionSavePort {

    override fun save(
        requester: FriendRequestUserInfo,
        receiver: FriendRequestUserInfo,
        type: FriendRestrictionType
    ): FriendRestriction {
        val newFriendRestrictionEntity = createNewFriendRelationshipEntities(
            requester.email, receiver.email, type
        )
        val result = springDataFriendRestrictionRepository.save(
            newFriendRestrictionEntity
        )

        return friendRestrictionMapper.mapToDomainEntity(result)
    }

    private fun createNewFriendRelationshipEntities(
        requesterEmail: String,
        receiverEmail: String,
        type: FriendRestrictionType
    ): FriendRestrictionJpaEntity {
        val users = fetchRequiredUsers(requesterEmail, receiverEmail)

        val requester = users.first { it.email == requesterEmail }
        val receiver = users.first { it.email == receiverEmail }

        return FriendRestrictionJpaEntity.create(receiver, requester, type)
    }

    private fun fetchRequiredUsers(vararg emails: String): List<UserJpaEntity> {
        val users = userRepository.findActiveUsersByEmails(emails[0], emails[1])
        if (users.size != emails.size) {
            throw BusinessException(UserErrorCode.USER_NOT_FOUND)
        }
        return users
    }
}