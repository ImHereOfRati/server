package com.kdongsu5509.friends.domain

import java.time.LocalDateTime

enum class FriendRestrictionType {
    REJECT { // 친구 요청 거절 - 30일 뒤 만료
        override fun expiryFrom(base: LocalDateTime): LocalDateTime? = base.plusDays(30)
    },
    BLOCK { // 친구 차단 - 영구
        override fun expiryFrom(base: LocalDateTime): LocalDateTime? = null
    };

    abstract fun expiryFrom(base: LocalDateTime): LocalDateTime?
}
