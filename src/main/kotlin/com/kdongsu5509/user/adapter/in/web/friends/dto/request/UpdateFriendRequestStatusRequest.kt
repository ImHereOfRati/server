package com.kdongsu5509.user.adapter.`in`.web.friends.dto.request

import jakarta.validation.constraints.NotBlank

data class UpdateFriendRequestStatusRequest(
    @field:NotBlank(message = "변경할 상태(ACCEPTED 또는 REJECTED)는 필수입니다.")
    val status: String
)
