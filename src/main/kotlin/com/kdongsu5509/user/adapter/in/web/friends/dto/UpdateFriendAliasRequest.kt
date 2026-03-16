package com.kdongsu5509.user.adapter.`in`.web.friends.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.Length
import java.util.*

data class UpdateFriendAliasRequest(
    @field:NotNull(message = "ì¹œêµ¬ ê´€ê³?ID???„ìˆ˜?…ë‹ˆ??")
    val friendRelationshipId: UUID,

    @field:NotBlank(message = "?ˆë¡œ??ì¹œêµ¬ ë³„ëª…?€ ?„ìˆ˜?…ë‹ˆ??")
    @field:Length(min = 1, max = 20, message = "ì¹œêµ¬ ë³„ëª…?€ 1 ~ 20?ê¹Œì§€ë§??…ë ¥ ê°€?¥í•©?ˆë‹¤.")
    val newFriendAlias: String
)
