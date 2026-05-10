package com.kdongsu5509.user.adapter.`in`.web.friends.dto.request

import com.fasterxml.jackson.annotation.JsonProperty
import org.hibernate.validator.constraints.Length

data class UpdateFriendRelationshipRequest(
    @field:Length(min = 1, max = 20, message = "친구 별명은 1자에서 20자 사이로 입력해주세요.")
    val alias: String? = null,

    @param:JsonProperty("blocked")
    val isBlocked: Boolean? = null
)
