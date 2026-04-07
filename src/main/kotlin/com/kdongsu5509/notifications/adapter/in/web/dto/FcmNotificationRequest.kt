package com.kdongsu5509.notifications.adapter.`in`.web.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class FcmNotificationRequest(
    @field:NotBlank
    @field:Email
    val receiverEmail: String,
    
    @field:NotBlank
    val type: String,
    
    @field:NotBlank
    val body: String
)
