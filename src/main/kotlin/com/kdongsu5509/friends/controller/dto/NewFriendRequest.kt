package com.kdongsu5509.friends.controller.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.Length
import java.util.*

data class NewFriendRequest(
    @field:NotNull(message = "요청 대상의 ID는 필수입니다.")
    val targetId: UUID,

    @field:NotBlank(message = "친구 요청 메시지는 필수입니다.")
    @field:Length(min = 10, max = 255, message = "친구 요청 메시지는 10자에서 255자 사이여야 합니다.")
    val message: String
)
