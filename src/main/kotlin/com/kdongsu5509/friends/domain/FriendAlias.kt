package com.kdongsu5509.friends.domain

data class FriendAlias(val value: String) {
    init {
        require(value.length <= 20) { "friendAlias는 20자를 넘을 수 없습니다." }
    }

    companion object {
        fun fromNickname(nickname: String): FriendAlias = FriendAlias(nickname.take(20))
    }
}
