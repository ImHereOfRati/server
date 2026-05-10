package com.kdongsu5509.user.application.dto

data class AlertResponse(
    val senderNickname: String,
    val body: String,
    val receiverEmail: String?
)
