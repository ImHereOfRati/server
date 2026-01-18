package com.kdongsu5509.imhereuserservice.domain

import java.util.*

data class Friend(
    val friendshipId: UUID,    // 요청 수락/거절/삭제 시 사용할 관계 ID
    val opponentId: UUID,      // 상대방 유저 고유 ID
    val opponentEmail: String,  // 상대방 이메일
    val opponentNickname: String, // 상대방 닉네임
    val status: FriendshipStatus  // 현재 관계 상태
)