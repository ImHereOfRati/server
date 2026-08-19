package com.kdongsu5509.notifications.domain

data class MessageSendResult(
    val status: String,
    val message: String,
    val providerMessageId: String? = null,
    val certainty: DeliveryCertainty = DeliveryCertainty.REJECTED,
) {
    val isSuccess: Boolean get() = certainty == DeliveryCertainty.CONFIRMED

    companion object {
        const val SUCCESS_STATUS = "200"
        const val FAIL_STATUS = "400"
        const val UNKNOWN_STATUS = FAIL_STATUS
        const val SUCCESS_MSG = "문자를 성공적으로 발송하였습니다."

        fun success(messageId: String? = null): MessageSendResult =
            MessageSendResult(SUCCESS_STATUS, SUCCESS_MSG, messageId, DeliveryCertainty.CONFIRMED)

        fun fail(errorMessage: String, messageId: String? = null): MessageSendResult =
            MessageSendResult(FAIL_STATUS, errorMessage, messageId, DeliveryCertainty.REJECTED)

        fun unknown(errorMessage: String, messageId: String? = null): MessageSendResult =
            MessageSendResult(UNKNOWN_STATUS, errorMessage, messageId, DeliveryCertainty.UNKNOWN)
    }
}

enum class DeliveryCertainty { CONFIRMED, REJECTED, UNKNOWN }
