package com.kdongsu5509.friends.api

import java.util.*

interface FriendAliasContract {

    fun findAlias(ownerId: UUID, targetId: UUID): String?
}
