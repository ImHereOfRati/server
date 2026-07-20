package com.kdongsu5509.terms.controller.dto

import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.service.TermCreateCommand
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME
import java.time.LocalDateTime

data class TermCreateRequest(
    @field:NotNull(message = "약관 종류는 필수입니다.")
    val type: TermTypes? = null,

    @field:NotBlank(message = "약관 제목은 필수입니다.")
    val title: String? = null,

    @field:NotBlank(message = "약관 내용은 필수입니다.")
    val content: String? = null,

    @field:DateTimeFormat(iso = DATE_TIME)
    @field:NotNull(message = "적용일은 필수입니다.")
    val effectiveDate: LocalDateTime? = null,

    @field:NotNull(message = "필수 여부는 빈 값일 수 없습니다")
    val isRequired: Boolean? = null,
) {
    fun toCommand(): TermCreateCommand = TermCreateCommand(
        type = requireNotNull(type),
        title = requireNotNull(title),
        content = requireNotNull(content),
        effectiveDate = requireNotNull(effectiveDate),
        isRequired = requireNotNull(isRequired),
    )
}
