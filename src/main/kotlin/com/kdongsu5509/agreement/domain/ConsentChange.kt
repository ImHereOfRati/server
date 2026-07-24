package com.kdongsu5509.agreement.domain

data class ConsentChange(
    val termId: Long,
    val action: AgreementStatus,
)
