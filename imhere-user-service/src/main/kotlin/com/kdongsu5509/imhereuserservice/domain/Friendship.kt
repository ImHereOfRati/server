package com.kdongsu5509.imhereuserservice.domain

import java.util.*

class Friendship(
    val id: UUID? = null,
    val requesterId: UUID,
    val receiverId: UUID,
    var status: FriendshipStatus = FriendshipStatus.PENDING
) {
    companion object {
        fun createRequest(requesterId: UUID, receiverId: UUID): Friendship {
            require(requesterId != receiverId) { "자기 자신에게는 친구 요청을 보낼 수 없습니다." }
            return Friendship(
                requesterId = requesterId,
                receiverId = receiverId,
                status = FriendshipStatus.PENDING
            )
        }
    }

    // 비즈니스 규칙 1: 수락 로직
    fun accept(currentUserId: UUID) {
        validateReceiver(currentUserId) // 수신자 본인인지 확인
        if (this.status != FriendshipStatus.PENDING) {
            throw IllegalStateException("이미 처리된 요청(상태: ${this.status})은 수락할 수 없습니다.")
        }
        this.status = FriendshipStatus.ACCEPTED
    }

    // 비즈니스 규칙 2: 거절 로직
    fun reject(currentUserId: UUID) {
        validateReceiver(currentUserId)
        if (this.status != FriendshipStatus.PENDING) {
            throw IllegalStateException("대기 중인 요청만 거절할 수 있습니다.")
        }
        this.status = FriendshipStatus.REJECTED
    }

    // 내부 검증: 요청을 받은 당사자만 결정권이 있음
    private fun validateReceiver(currentUserId: UUID) {
        if (this.receiverId != currentUserId) {
            throw IllegalArgumentException("본인에게 온 요청만 처리할 수 있습니다.")
        }
    }
}