package com.kdongsu5509.user.adapter.`in`.web.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class NicknameChangeRequest(
    @field:NotBlank(message = "새로운 닉네임은 필수입니다.")
    @field:Size(max = 5, message = "닉네임은 최대 5자까지 가능합니다.")
    val newNickname: String
)
