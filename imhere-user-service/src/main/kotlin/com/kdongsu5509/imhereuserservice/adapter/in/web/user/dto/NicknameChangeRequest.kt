package com.kdongsu5509.imhereuserservice.adapter.`in`.web.user.dto

import jakarta.validation.constraints.NotBlank

data class NicknameChangeRequest(
    @field:NotBlank(message = "새로운 닉네임은 필수입니다")
    val newNickname: String
)