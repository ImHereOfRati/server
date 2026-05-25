package com.kdongsu5509.terms.controller.dto

import com.kdongsu5509.terms.domain.TermTypes
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class TermCreateRequest(
    @field:NotNull(message = "약관 종류는 필수입니다.")
    val type: TermTypes,

    @field:NotBlank(message = "약관 제목은 필수입니다.")
    val title: String,

    @field:NotBlank(message = "약관 내용은 필수입니다.")
    val content: String,

    @field:NotNull(message = "적용일은 필수입니다.")
    var effectiveDate: LocalDateTime,

    @field:NotNull(message = "필수 여부는 빈 값일 수 없습니다")
    var isRequired: Boolean
)
