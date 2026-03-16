package com.kdongsu5509.user.adapter.`in`.web.user.dto

import jakarta.validation.constraints.NotBlank

data class NicknameChangeRequest(
    @field:NotBlank(message = "?ˆë¡œ???‰ë„¤?„ì? ?„ìˆ˜?…ë‹ˆ??)
    val newNickname: String
)
