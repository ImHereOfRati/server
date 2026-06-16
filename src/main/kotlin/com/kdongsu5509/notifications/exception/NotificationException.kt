package com.kdongsu5509.notifications.exception

import com.kdongsu5509.support.exception.CommonErrorCode
import com.kdongsu5509.support.exception.ImHereBaseErrorCode
import org.springframework.http.HttpStatus

/**
 * 알림 및 메시지 관련 비즈니스 에러 (NOTIFICATION / SMS)
 */
enum class NotificationException(
    category: CommonErrorCode,
    override val imhereErrorCode: String,
    override val errorMessage: String
) : ImHereBaseErrorCode {
    // --- 0xx: Bad Request (400) ---
    SMS_NOT_ALLOW_EMPTY(CommonErrorCode.INVALID_INPUT, "SMS-000", "전송할 메시지 내용이 비어있습니다."),
    SMS_BODY_TOO_LONG(CommonErrorCode.INVALID_INPUT, "SMS-001", "SMS 본문은 개행 포함 45자를 초과할 수 없습니다."),
    UNSUPPORTED_TARGET_TYPE(CommonErrorCode.INVALID_INPUT, "NOTI-001", "지원하지 않는 알림 수단입니다."),

    // ---2xx: UNAUTHORIZE(403) ---
    NOT_MY_NOTIFICATION(CommonErrorCode.FORBIDDEN, "NOTI-200", "해당 알람에 대한 작업 권한이 없습니다."),

    // --- 3xx: Not Found (404) ---
    NOTIFICATION_NOT_FOUND(CommonErrorCode.NOT_FOUND, "NOTI-300", "요청한 알림을 찾을 수 없습니다"),
    FCM_TOKEN_NOT_FOUND(CommonErrorCode.NOT_FOUND, "FCM-300", "FCM 토큰을 찾을 수 없습니다."),
    FCM_TOKEN_UNREGISTERED(CommonErrorCode.NOT_FOUND, "FCM-301", "등록되지 않았거나 만료된 FCM 토큰입니다."),

    // --- 9xx: Internal Error (500) ---
    FCM_INVALID_ARGUMENT(CommonErrorCode.INTERNAL_SERVER_ERROR, "FCM-900", "FCM 메시지 구성이 올바르지 않습니다."),
    FCM_AUTH_ERROR(CommonErrorCode.INTERNAL_SERVER_ERROR, "FCM-901", "FCM 인증 서버와의 통신 중 오류가 발생했습니다."),
    FCM_UNKNOWN_ERROR(CommonErrorCode.INTERNAL_SERVER_ERROR, "FCM-902", "알 수 없는 FCM 오류가 발생했습니다."),
    SMS_SEND_FAILED(CommonErrorCode.INTERNAL_SERVER_ERROR, "SMS-900", "SMS 전송 중 외부 서비스 오류가 발생했습니다.");

    override val httpStatus: HttpStatus = category.httpStatus
}
