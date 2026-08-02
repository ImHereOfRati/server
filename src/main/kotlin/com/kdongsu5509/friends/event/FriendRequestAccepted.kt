package com.kdongsu5509.friends.event

import com.kdongsu5509.shared.event.DomainEvent
import java.time.LocalDateTime
import java.util.UUID

data class FriendRequestAccepted(
    val accepterId: UUID,
    val accepterNickname: String,
    val requesterId: UUID,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent
