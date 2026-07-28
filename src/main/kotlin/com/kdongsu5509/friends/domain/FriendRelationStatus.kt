package com.kdongsu5509.friends.domain

enum class FriendRelationStatus {
    REQUESTED, // 요청 보낸 상태
    ACCEPTED, //서로 친구인 상태
    REJECTED, // 요청이 거절된 상태
    BLOCKED, // 한 쪽이 차단한 상태
    CANCEL // 취소 상태
}
