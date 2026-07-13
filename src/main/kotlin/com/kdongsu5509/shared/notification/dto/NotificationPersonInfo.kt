package com.kdongsu5509.shared.notification.dto

/**
 * 알림 발송/수신 대상자 정보(wire DTO).
 *
 * [email]은 형식 불변식을 스스로 지킨다(경계 진입 즉시 자기검증) — 필드 타입은 String을 유지해
 * 타 모듈 생성 계약을 깨지 않으면서 잘못된 값을 조기에 거른다(primitive obsession 완화).
 */
data class NotificationPersonInfo(
    val email: String,
    val nickname: String
) {
    init {
        require(email.isNotBlank() && email.contains("@")) {
            "유효하지 않은 이메일입니다: '$email'"
        }
    }
}
