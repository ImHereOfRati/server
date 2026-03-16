package com.kdongsu5509.user.adapter.`in`.web.friends.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.Length
import java.util.*

data class CreateFriendRequest(
    @field:NotNull(message = "?ë?ë°?ID???„ìˆ˜?…ë‹ˆ??")
    val receiverId: UUID,

    @field:NotBlank(message = "?”ì²­ ë©”ì‹œì§€???„ìˆ˜?…ë‹ˆ??")
    @field:Length(min = 1, max = 255, message = "?”ì²­ ë©”ì‹œì§€??1 ~ 255???¬ì´ë¥??…ë ¥?˜ì—¬???©ë‹ˆ??)
    val message: String
)
