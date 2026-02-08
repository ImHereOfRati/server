package com.kdongsu5509.imhereuserservice.domain.user

import java.time.LocalDateTime
import java.util.*

/**
 * 저장 완료 후 결과로 받는 도메인 객체
 */
data class UserAgreement(
    val agreementId: UUID,
    val userEmail: String,
    val termsVersionId: Long,
    val agreedAt: LocalDateTime
)