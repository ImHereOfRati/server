package com.kdongsu5509.agreement.controller.dto

import com.kdongsu5509.agreement.service.dto.AgreementHistoryResult
import java.time.LocalDateTime

data class AgreementHistoryResponse(
    val termId: Long,
    val action: String,
    val occurredAt: LocalDateTime,
) {
    companion object {
        fun from(result: AgreementHistoryResult): AgreementHistoryResponse =
            AgreementHistoryResponse(
                termId = result.termId,
                action = result.action.name,
                occurredAt = result.occurredAt,
            )
    }
}
