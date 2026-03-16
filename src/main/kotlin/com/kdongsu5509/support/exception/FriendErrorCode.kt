package com.kdongsu5509.support.exception

import org.springframework.http.HttpStatus

enum class FriendErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String
) : BaseErrorCode {
    SELF_FRIENDSHIP(
        HttpStatus.BAD_REQUEST,
        "FRIEND_REQUEST_001",
        "자기 자신에게 친구 요청을 보낼 수 없습니다."
    ),
    FRIENDSHIP_REQUEST_MESSAGE_OVER(
        HttpStatus.BAD_REQUEST,
        "FRIEND_REQUEST_002",
        "친구 요청 메시지의 길이는 최대 255자까지 가능합니다."
    ),
    FRIENDSHIP_REQUEST_NOT_FOUND(
        HttpStatus.BAD_REQUEST,
        "FRIEND_REQUEST_003",
        "해당 친구 요청은 존재하지 않습니다."
    ),
    FRIENDSHIP_REQUEST_RECEIVER_MISS_MATCH(
        HttpStatus.BAD_REQUEST,
        "FRIEND_REQUEST_004",
        "해당 친구 요청은 나에게 온 요청이 아닙니다."
    ),
    FRIEND_RESTRICTION_NOT_FOUND(
        HttpStatus.BAD_REQUEST,
        "FRIEND_RESTRICTION_001",
        "해당 거절/차단 관계를 찾을 수 없습니다."
    ),
    FRIEND_RESTRICTION_ACTOR_MISS_MATCH(
        HttpStatus.BAD_REQUEST,
        "FRIEND_RESTRICTION_002",
        "해당 거절/차단 관계의 처리 권한이 없습니다."
    ),
    FRIEND_RELATIONSHIP_NOT_FOUND(
        HttpStatus.BAD_REQUEST,
        "FRIEND_RELATIONSHIP_001",
        "해당 친구 관계는 존재하지 않습니다."
    ),
    FRIEND_RELATIONSHIP_OWNER_MISS_MATCH(
        HttpStatus.BAD_REQUEST,
        "FRIEND_RELATIONSHIP_002",
        "해당 친구 관계의 처리 권한이 없습니다."
    ),
}
