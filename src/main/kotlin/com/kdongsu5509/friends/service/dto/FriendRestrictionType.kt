package com.kdongsu5509.friends.service.dto

import com.kdongsu5509.friends.domain.FriendRelationStatus

/**
 * 제한의 종류. 클라이언트 응답과 관리자 화면에 그대로 노출되는 값이라 문자열을 바꿀 수 없다.
 *
 * 도메인에서는 [FriendRelationStatus]의 REJECTED / BLOCKED로 흡수됐지만,
 * 기존 API가 REJECT / BLOCK을 내보내고 있어 표현 계층에만 남긴다.
 */
enum class FriendRestrictionType {
    REJECT,
    BLOCK;

    companion object {
        fun from(status: FriendRelationStatus): FriendRestrictionType = when (status) {
            FriendRelationStatus.REJECTED -> REJECT
            FriendRelationStatus.BLOCKED -> BLOCK
            else -> BLOCK
        }
    }
}
