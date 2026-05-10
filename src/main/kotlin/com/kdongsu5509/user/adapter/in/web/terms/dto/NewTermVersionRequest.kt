package com.kdongsu5509.user.adapter.`in`.web.terms.dto

import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDateTime

data class NewTermVersionRequest(
    @field:NotNull(message = "약관 정의 ID는 필수입니다.")
    @field:Positive(message = "올바른 약관 정의 ID를 입력해주세요.")
    val termDefinitionId: Long,

    @field:NotBlank(message = "약관 버전 정보는 필수입니다.")
    val version: String,

    @field:NotBlank(message = "약관 내용은 필수입니다.")
    val content: String,

    @field:NotNull(message = "약관 시행일은 필수입니다.")
    @field:FutureOrPresent(message = "시행일은 현재 또는 미래 날짜여야 합니다.")
    val effectiveDate: LocalDateTime
)