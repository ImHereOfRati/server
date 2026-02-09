package com.kdongsu5509.imhereuserservice.adapter.`in`.web.friends.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.Length
import java.util.*

data class CreateFriendRequest(
    @field:NotNull(message = "상대방 ID는 필수입니다.")
    val receiverId: UUID,

    @field:NotBlank(message = "요청 메시지는 필수입니다.")
    @field:Length(min = 1, max = 255, message = "요청 메시지는 1 ~ 255자 사이를 입력하여야 합니다")
    val message: String
)