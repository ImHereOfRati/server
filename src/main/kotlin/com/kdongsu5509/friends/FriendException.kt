package com.kdongsu5509.friends

import com.kdongsu5509.support.exception.CommonErrorCode
import com.kdongsu5509.support.exception.ImHereBaseErrorCode

enum class FriendException(
    category: CommonErrorCode,
    override val imhereErrorCode: String,
    override val errorMessage: String
) : ImHereBaseErrorCode {
    // --- 0xx: Bad Request (400) ---
    SELF_FRIENDSHIP(CommonErrorCode.INVALID_INPUT, "FRIEND-000", "자신에게는 친구 요청을 보낼 수 없습니다."),
    FRIEND_ALIAS_TOO_LONG(CommonErrorCode.INVALID_INPUT, "FRIEND-001", "친구 별칭은 10자를 넘을 수 없습니다."),
    FRIENDSHIP_REQUEST_RECEIVER_MISS_MATCH(CommonErrorCode.INVALID_INPUT, "FRIEND-002", "본인에게 온 친구 요청이 아닙니다."),
    FRIEND_ALIAS_BLANK(CommonErrorCode.INVALID_INPUT, "FRIEND-003", "친구 별칭은 비어있을 수 없습니다."),
    REQUEST_MESSAGE_SIZE_MORE_THAN_TEN(CommonErrorCode.INVALID_INPUT, "FRIEND-004", "친구 요청 메시지는 10자 이상이어야 합니다."),
    FRIENDSHIP_NOT_ACCEPTED(CommonErrorCode.INVALID_INPUT, "FRIEND-005", "아직 친구가 아닙니다."),
    FRIEND_REQUEST_ALREADY_HANDLED(CommonErrorCode.INVALID_INPUT, "FRIEND-006", "이미 처리된 친구 요청입니다."),
    SELF_BLOCK(CommonErrorCode.INVALID_INPUT, "FRIEND-007", "자기 자신은 차단할 수 없습니다."),
    FRIENDSHIP_UNBLOCKED(CommonErrorCode.INVALID_INPUT, "FRIEND-008", "차단된 사용자만 차단 해제할 수 있습니다"),

    // --- 2xx: Forbidden (403) ---
    FRIEND_RELATIONSHIP_OWNER_MISS_MATCH(CommonErrorCode.FORBIDDEN, "FRIEND-200", "해당 친구 관계를 관리할 권한이 없습니다."),

    // --- 3xx: Not Found (404) ---
    FRIEND_RELATIONSHIP_NOT_FOUND(CommonErrorCode.NOT_FOUND, "FRIEND-300", "해당 친구 관계가 존재하지 않습니다."),
    BLOCK_TARGET_NOT_FOUND(CommonErrorCode.NOT_FOUND, "FRIEND-301", "차단할 사용자를 찾을 수 없습니다."),

    // --- 5xx: Conflict (409) ---
    ALREADY_FRIEND(CommonErrorCode.CONFLICT, "FRIEND-500", "이미 친구 관계입니다."),
    FRIEND_REQUEST_ALREADY_SENT(CommonErrorCode.CONFLICT, "FRIEND-501", "이미 친구 요청을 보낸 상태입니다."),

    // --- 7xx: Unprocessable Entity (422) ---
    FRIEND_REQUEST_UNPROCESSABLE(
        CommonErrorCode.UNPROCESSABLE_ENTITY,
        "FRIEND-700",
        "차단 혹은 과거에 거절된 관계의 친구에게 친구 요청을 할 수 없습니다."
    );

    override val httpStatus = category.httpStatus
}
