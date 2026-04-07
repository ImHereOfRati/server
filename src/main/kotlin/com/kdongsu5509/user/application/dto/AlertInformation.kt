package com.kdongsu5509.user.application.dto

data class AlertInformation(
    val senderNickname: String,
    val body: String,
    val receiverEmail: String?
)
