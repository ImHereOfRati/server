package com.kdongsu5509.friends

import com.kdongsu5509.support.exception.CommonErrorCode
import com.kdongsu5509.support.exception.ImHereBaseErrorCode
import org.springframework.http.HttpStatus

enum class FriendException(
    category: CommonErrorCode,
    override val imhereErrorCode: String,
    override val errorMessage: String
) : ImHereBaseErrorCode {
    // --- 0xx: Bad Request (400) ---
    SELF_FRIENDSHIP(CommonErrorCode.INVALID_INPUT, "FRIEND-000", "자신에게는 친구 요청을 보낼 수 없습니다."),
    FRIENDSHIP_REQUEST_RECEIVER_MISS_MATCH(CommonErrorCode.INVALID_INPUT, "FRIEND-002", "본인에게 온 친구 요청이 아닙니다."),

    // --- 2xx: Forbidden (403) ---
    FRIEND_RELATIONSHIP_OWNER_MISS_MATCH(CommonErrorCode.FORBIDDEN, "FRIEND-200", "해당 친구 관계를 관리할 권한이 없습니다."),

    // --- 3xx: Not Found (404) ---
    FRIEND_REQUEST_NOT_FOUND(CommonErrorCode.NOT_FOUND, "FRIEND-300", "해당 친구 요청을 찾을 수 없습니다."),
    FRI_RESTRICTION_NOT_FOUND(CommonErrorCode.NOT_FOUND, "FRIEND-301", "차단 또는 제한 정보를 찾을 수 없습니다."),
    FRIEND_RELATIONSHIP_NOT_FOUND(CommonErrorCode.NOT_FOUND, "FRIEND-302", "해당 친구 관계가 존재하지 않습니다."),

    // --- 5xx: Conflict (409) ---
    ALREADY_FRIEND(CommonErrorCode.CONFLICT, "FRIEND-500", "이미 친구 관계입니다."),
    FRIEND_REQUEST_ALREADY_SENT(CommonErrorCode.CONFLICT, "FRIEND-501", "이미 친구 요청을 보낸 상태입니다."),

    // --- 7xx: Unprocessable Entity (422) ---
    FRIEND_REQUEST_UNPROCESSABLE_BY_ME(
        CommonErrorCode.UNPROCESSABLE_ENTITY,
        "FRIEND-700",
        "내가 차단 혹은 거절한 친구에게 친구 요청을 할 수 없습니다."
    ),
    FRIEND_REQUEST_UNPROCESSABLE_BY_TARGET(
        CommonErrorCode.UNPROCESSABLE_ENTITY,
        "FRIEND-701",
        "나를 차단 혹은 거절한 친구에게 친구 요청을 할 수 없습니다."
    );

    override val httpStatus: HttpStatus = category.httpStatus
}
