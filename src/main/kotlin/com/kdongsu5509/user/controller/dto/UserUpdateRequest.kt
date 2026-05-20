package com.kdongsu5509.user.controller.dto

import jakarta.validation.constraints.Size

data class UserUpdateRequest(
    @field:Size(max = 5, message = "닉네임은 최대 5자까지 가능합니다.")
    val nickname: String? = null
)
