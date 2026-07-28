package com.kdongsu5509.friends.service

import com.kdongsu5509.friends.domain.FriendRelation
import com.kdongsu5509.friends.service.dto.FriendMember
import com.kdongsu5509.user.api.UserLookupContract
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
import org.springframework.stereotype.Component
import java.util.*

@Component
class FriendMemberLoader(
    private val userLookupContract: UserLookupContract
) {
    fun of(relation: FriendRelation): Map<UUID, FriendMember> = of(listOf(relation))

    fun of(relations: Collection<FriendRelation>): Map<UUID, FriendMember> {
        if (relations.isEmpty()) return emptyMap()

        val ids = relations.flatMapTo(mutableSetOf()) { listOf(it.pair.low, it.pair.high) }
        return userLookupContract.findAllByIds(ids).associate { it.id to FriendMember.from(it) }
    }

    fun <V : Any> toViews(
        relations: Slice<FriendRelation>,
        toView: (FriendRelation, Map<UUID, FriendMember>) -> V
    ): Slice<V> {
        val members = of(relations.content)
        val views = relations.content
            .filter { members.containsKey(it.pair.low) && members.containsKey(it.pair.high) }
            .map { toView(it, members) }

        return SliceImpl(views, relations.pageable, relations.hasNext())
    }
}
