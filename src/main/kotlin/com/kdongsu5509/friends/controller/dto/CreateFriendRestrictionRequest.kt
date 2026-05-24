package com.kdongsu5509.friends.controller.dto

import jakarta.validation.constraints.NotNull
import java.util.UUID

data class CreateFriendRestrictionRequest(
    @field:NotNull(message = "차단할 대상 유저의 식별자는 필수입니다.")
    val targetUserId: UUID
)
