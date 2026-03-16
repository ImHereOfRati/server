package com.kdongsu5509.user.adapter.`in`.web.terms.dto

import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

data class NewTermVersionRequest(
    @field:NotNull(message = "?½ê? ?•ì˜ ID???„ìˆ˜?…ë‹ˆ??")
    @field:Positive
    var termDefinitionId: Long,

    @field:NotBlank(message = "ë²„ì „ ?•ë³´???„ìˆ˜?…ë‹ˆ??")
    val version: String,

    @field:NotBlank(message = "?½ê? ë³¸ë¬¸ ?´ìš©?€ ?„ìˆ˜?…ë‹ˆ??")
    val content: String,

    @field:NotNull(message = "?œí–‰???¤ì •?€ ?„ìˆ˜?…ë‹ˆ??")
    @field:FutureOrPresent(message = "?œí–‰?¼ì? ?„ì¬ ?ëŠ” ë¯¸ë˜ ? ì§œ?¬ì•¼ ?©ë‹ˆ??")
    var effectiveDate: LocalDateTime
)
