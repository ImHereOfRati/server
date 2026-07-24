package com.kdongsu5509.agreement.service.dto

import com.kdongsu5509.agreement.domain.AgreementStatus
import java.time.LocalDateTime

data class AgreementHistoryResult(
    val termId: Long,
    val action: AgreementStatus,
    val occurredAt: LocalDateTime,
)
