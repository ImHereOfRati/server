package com.kdongsu5509.user.domain.user

import java.time.LocalDateTime
import java.util.*

data class UserAgreement(
    val agreementId: UUID,
    val userEmail: String,
    val termsVersionId: Long,
    val agreedAt: LocalDateTime
)
