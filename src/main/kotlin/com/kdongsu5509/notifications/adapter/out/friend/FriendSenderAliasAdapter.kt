package com.kdongsu5509.notifications.adapter.out.friend

import com.kdongsu5509.friends.api.FriendAliasContract
import com.kdongsu5509.notifications.application.port.out.SenderAliasPort
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class FriendSenderAliasAdapter(
    private val friendAliasContract: FriendAliasContract,
) : SenderAliasPort {

    override fun findAlias(ownerId: UUID, senderId: UUID): String? =
        friendAliasContract.findAlias(ownerId, senderId)
}
