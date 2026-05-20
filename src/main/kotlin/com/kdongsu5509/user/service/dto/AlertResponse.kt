package com.kdongsu5509.user.service.dto

data class AlertResponse(
    val senderNickname: String,
    val body: String,
    val receiverEmail: String?
)
