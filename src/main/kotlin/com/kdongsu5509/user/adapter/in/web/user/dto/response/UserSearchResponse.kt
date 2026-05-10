package com.kdongsu5509.user.adapter.`in`.web.user.dto.response

import java.util.*

data class UserSearchResponse(
    val userId: UUID,
    val userEmail: String,
    val userNickname: String
)
