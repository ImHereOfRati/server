package com.kdongsu5509.user.adapter.`in`.web.friends.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.Length
import java.util.*

data class CreateFriendRequest(
    @field:NotNull(message = "수신자 ID는 필수입니다")
    val receiverId: UUID,

    @field:NotBlank(message = "공백은 허용되지 않습니다")
    @field:Email
    val receiverEmail: String,

    @field:NotBlank(message = "공백은 허용되지 않습니다")
    @field:Length(min = 10, max = 255, message = "요청 메시지는 10 ~ 255자 사이여야 합니다")
    val message: String
)
