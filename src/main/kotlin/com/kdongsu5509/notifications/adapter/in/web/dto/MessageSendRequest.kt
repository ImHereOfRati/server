package com.kdongsu5509.notifications.adapter.`in`.web.dto

import org.jetbrains.annotations.NotNull

data class MessageSendRequest(
    @param:NotNull
    val receiverNumber: String,
    @param:NotNull
    val location: String
)

//TODO : FCM -> 100자 제한
