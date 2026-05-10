package com.kdongsu5509.user.exception

import com.kdongsu5509.support.exception.BusinessCode
import com.kdongsu5509.support.exception.ErrorReason

/**
 * 친구 도메인 관련 비즈니스 에러 (FRIEND)
 *
 * [번호 체계 가이드]
 * - 0xx: 400 Bad Request (요청 데이터 오류)
 * - 1xx: 403 Forbidden (권한 부족)
 * - 2xx: 404 Not Found (요청/관계 부재)
 * - 3xx: 409 Conflict (상태 충돌)
 */
enum class FriendError(
    override val errorCategory: ErrorReason,
    override val businessCode: String,
    override val message: String? = null
) : BusinessCode {
    // --- 0xx: Bad Request (400) ---
    SELF_FRIENDSHIP(ErrorReason.INVALID_INPUT, "FRIEND_001", "자신에게는 친구 요청을 보낼 수 없습니다."),
    FRIENDSHIP_REQUEST_MESSAGE_OVER(ErrorReason.INVALID_INPUT, "FRIEND_002", "메시지 길이가 제한을 초과했습니다."),
    FRIENDSHIP_REQUEST_RECEIVER_MISS_MATCH(ErrorReason.INVALID_INPUT, "FRIEND_003", "본인에게 온 친구 요청이 아닙니다."),
    FRI_RESTRICTION_ACTOR_MISS_MATCH(ErrorReason.INVALID_INPUT, "FRIEND_004", "해당 작업을 수행할 권한이 없습니다."),

    // --- 1xx: Permission (403) ---
    FRIEND_RELATIONSHIP_OWNER_MISS_MATCH(ErrorReason.FORBIDDEN, "FRIEND_101", "해당 친구 관계를 관리할 권한이 없습니다."),

    // --- 2xx: Resource Absence (404) ---
    FRIENDSHIP_REQUEST_NOT_FOUND(ErrorReason.NOT_FOUND, "FRIEND_201", "해당 친구 요청을 찾을 수 없습니다."),
    FRI_RESTRICTION_NOT_FOUND(ErrorReason.NOT_FOUND, "FRIEND_202", "차단 또는 제한 정보를 찾을 수 없습니다."),
    FRIEND_RELATIONSHIP_NOT_FOUND(ErrorReason.NOT_FOUND, "FRIEND_203", "해당 친구 관계가 존재하지 않습니다."),

    // --- 3xx: State Conflict (409) ---
    ALREADY_FRIEND(ErrorReason.CONFLICT, "FRIEND_301", "이미 친구 관계입니다."),
    FRIEND_REQUEST_ALREADY_SENT(ErrorReason.CONFLICT, "FRIEND_302", "이미 친구 요청을 보낸 상태입니다.")
}
