package com.kdongsu5509.friends.event

import com.kdongsu5509.shared.event.DomainEvent
import java.time.LocalDateTime
import java.util.UUID

data class FriendRequestSent(
    val requesterId: UUID,
    val requesterNickname: String,
    val receiverId: UUID,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent
