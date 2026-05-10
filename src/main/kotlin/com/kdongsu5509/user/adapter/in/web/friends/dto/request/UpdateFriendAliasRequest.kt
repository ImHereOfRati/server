package com.kdongsu5509.user.adapter.`in`.web.friends.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.Length
import java.util.*

data class UpdateFriendAliasRequest(
    @field:NotNull(message = "친구 관계 ID는 필수입니다.")
    val friendRelationshipId: UUID,

    @field:NotBlank(message = "변경할 친구 별명은 필수입니다.")
    @field:Length(min = 1, max = 20, message = "친구 별명은 1자에서 20자 사이로 입력해주세요.")
    val newFriendAlias: String
)
