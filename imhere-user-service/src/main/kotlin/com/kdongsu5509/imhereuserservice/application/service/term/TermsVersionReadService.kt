package com.kdongsu5509.imhereuserservice.application.service.term

import com.kdongsu5509.imhereuserservice.adapter.`in`.web.terms.dto.TermVersionResponse
import com.kdongsu5509.imhereuserservice.application.port.`in`.terms.ReadTermsVersionUseCase
import com.kdongsu5509.imhereuserservice.application.port.out.term.TermsVersionLoadPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class TermsVersionReadService(
    private val termsVersionLoadPort: TermsVersionLoadPort
) : ReadTermsVersionUseCase {
    override fun read(termDefinitionId: Long): TermVersionResponse {
        return TermVersionResponse.from(termsVersionLoadPort.loadSpecificActiveTermVersion(termDefinitionId))
    }
}