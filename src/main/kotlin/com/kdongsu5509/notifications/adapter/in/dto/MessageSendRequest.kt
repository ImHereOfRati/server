package com.kdongsu5509.imhere.message.adapter.dto

import org.jetbrains.annotations.NotNull

data class MessageSendRequest(
    @NotNull
    val message: String,

    @NotNull
    val receiverNumber: String
)
