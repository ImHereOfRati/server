package com.kdongsu5509.imhereuserservice.adapter.`in`.web.user.dto

import java.util.*

data class UserSearchResponse(
    val userId: UUID,
    val userEmail: String,
    val userNickname: String
)