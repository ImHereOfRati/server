package com.kdongsu5509.notifications.application.port.out

import java.util.UUID

interface SenderAliasPort {

    fun findAlias(ownerId: UUID, senderId: UUID): String?
}
