package com.kdongsu5509.friends.domain

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.support.exception.throwIt

data class FriendAlias(val value: String) {
    init {
        if (value.isBlank()) FriendException.FRIEND_ALIAS_BLANK.throwIt()
        if (value.length > MAX_LENGTH) FriendException.FRIEND_ALIAS_TOO_LONG.throwIt()
    }

    companion object {
        const val MAX_LENGTH = 10
    }
}
