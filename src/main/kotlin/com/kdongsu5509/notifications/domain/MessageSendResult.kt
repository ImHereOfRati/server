package com.kdongsu5509.notifications.domain

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
