package com.kdongsu5509.notifications.domain

/**
 * 외부 메시지(SMS 등) 발송 결과 값 객체.
 *
 * 성공 여부 판정([isSuccess])을 스스로 안다. 외부 SDK(Solapi 등)의 응답 스키마에 독립적이며,
 * 어댑터가 벤더 응답을 이 값으로 변환해 반환한다(포트가 어댑터 타입에 의존하지 않도록 — DIP).
 */
data class MessageSendResult(
    val status: String,
    val message: String,
) {
    val isSuccess: Boolean get() = status == SUCCESS_STATUS

    companion object {
        const val SUCCESS_STATUS = "200"
        const val FAIL_STATUS = "400"
        const val SUCCESS_MSG = "정상적으로 문자를 발송하였습니다"

        fun success(): MessageSendResult = MessageSendResult(SUCCESS_STATUS, SUCCESS_MSG)

        fun fail(errorMessage: String): MessageSendResult = MessageSendResult(FAIL_STATUS, errorMessage)
    }
}
