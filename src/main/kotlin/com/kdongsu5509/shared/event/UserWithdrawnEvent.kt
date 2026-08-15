package com.kdongsu5509.shared.event

import java.time.LocalDateTime
import java.util.UUID

/** Published after a user is marked withdrawn so owning modules can purge data. */
data class UserWithdrawnEvent(
    val userId: UUID,
    override val eventId: UUID = UUID.randomUUID(),
    override val occurredAt: LocalDateTime = LocalDateTime.now(),
) : DomainEvent
