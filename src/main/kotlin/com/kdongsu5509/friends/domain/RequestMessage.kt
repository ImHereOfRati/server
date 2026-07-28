package com.kdongsu5509.friends.domain

import com.kdongsu5509.friends.FriendException
import com.kdongsu5509.support.exception.throwIt

data class RequestMessage(val value: String) {
    init {
        if (value.isBlank() || value.length < 10)
            FriendException.REQUEST_MESSAGE_SIZE_MORE_THAN_TEN.throwIt()
    }
}
