package com.kdongsu5509.friends.controller.dto

import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

data class UpdateAliasRequest(
    @field:NotBlank(message = "Alias is required.")
    @field:Length(max = 20, message = "Alias must be 20 characters or less.")
    val alias: String
)
