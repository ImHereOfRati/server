package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.controller.dto.FriendRequestViewType
import com.kdongsu5509.friends.domain.FriendRequest
import com.kdongsu5509.friends.domain.FriendRestriction
import com.kdongsu5509.friends.domain.Friendship
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import java.util.*

interface FriendRequestService {
    fun request(requesterEmail: String, receiverId: UUID, message: String): FriendRequest
    fun findAll(pageable: Pageable): Slice<FriendRequest>
    fun findAllByEmailAndType(email: String, type: FriendRequestViewType, pageable: Pageable): Slice<FriendRequest>
    fun findById(id: UUID): FriendRequest
    fun findByIdAndParticipantEmail(id: UUID, participantEmail: String): FriendRequest
    fun acceptRequest(email: String, id: UUID): Friendship
    fun rejectRequest(email: String, id: UUID): FriendRestriction
    fun deleteById(id: UUID)
    fun deleteByIdAndReceiverEmail(id: UUID, receiverEmail: String)
    fun deleteByIdAndRequesterEmail(id: UUID, requesterEmail: String)
}
