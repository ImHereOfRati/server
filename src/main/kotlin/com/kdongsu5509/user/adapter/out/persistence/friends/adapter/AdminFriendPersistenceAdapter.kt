package com.kdongsu5509.user.adapter.out.persistence.friends.adapter

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.UserErrorCode
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRelationshipsRepository
import com.kdongsu5509.user.adapter.out.persistence.friends.jpa.SpringDataFriendRequestRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.UserJpaEntity
import com.kdongsu5509.user.application.port.out.friend.AdminFriendPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminFriendPersistenceAdapter(
    private val userRepository: SpringQueryDSLUserRepository,
    private val relationshipsRepository: SpringDataFriendRelationshipsRepository,
    private val requestRepository: SpringDataFriendRequestRepository
) : AdminFriendPort {

    @Transactional
    override fun forceClearFriendRelationship(userAEmail: String, userBEmail: String) {
        val userA = fetchUser(userAEmail)
        val userB = fetchUser(userBEmail)
        relationshipsRepository.deleteByOwnerUserAndFriendUser(userA, userB)
        relationshipsRepository.deleteByOwnerUserAndFriendUser(userB, userA)
    }

    @Transactional
    override fun forceClearFriendRequest(requesterEmail: String, receiverEmail: String) {
        val requester = fetchUser(requesterEmail)
        val receiver = fetchUser(receiverEmail)
        requestRepository.deleteByRequesterAndReceiver(requester, receiver)
    }

    private fun fetchUser(email: String): UserJpaEntity =
        userRepository.findUserByEmail(email).orElseThrow {
            BusinessException(UserErrorCode.USER_NOT_FOUND)
        }
}
