package com.kdongsu5509.user.service

import com.kdongsu5509.user.domain.User
import com.kdongsu5509.user.service.dto.MultiTermsConsentCommand

/**
 * 약관 동의 유스케이스 추상(포트).
 * auth 등 소비 모듈이 구체 구현(UserAgreementService)이 아닌 이 추상에만 의존하도록 한다(DIP / 진단 C3).
 */
interface UserAgreementUseCase {
    fun consentAll(email: String, multiTermsConsentCommand: MultiTermsConsentCommand): User
    fun consent(email: String, id: Long)
}
