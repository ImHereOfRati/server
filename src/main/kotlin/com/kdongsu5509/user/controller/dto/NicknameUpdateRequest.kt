package com.kdongsu5509.user.controller.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class NicknameUpdateRequest(
    @field:NotBlank("변경할 닉네임 입력해주세요")
    @field:Size(min = 2, max = 7, message = "닉네임은 최소 2글자, 최대 7자까지 가능합니다.")
    val nickname: String
)
