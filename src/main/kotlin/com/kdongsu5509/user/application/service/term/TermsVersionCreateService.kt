package com.kdongsu5509.user.application.service.term

import com.kdongsu5509.user.adapter.`in`.web.terms.dto.NewTermVersionRequest
import com.kdongsu5509.user.application.dto.AlertInformation
import com.kdongsu5509.user.application.port.`in`.terms.CreateTermVersionUseCase
import com.kdongsu5509.user.application.port.out.noti.TermAlertPort
import com.kdongsu5509.user.application.port.out.term.TermsVersionSavePort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional
class TermsVersionCreateService(
    private val termsVersionSavePort: TermsVersionSavePort,
    private val termAlertPort: TermAlertPort
) : CreateTermVersionUseCase {
    override fun createNewTermVersion(newTermVersionRequest: NewTermVersionRequest) {
        termsVersionSavePort.saveTermVersion(
            newTermVersionRequest.termDefinitionId,
            newTermVersionRequest.version,
            newTermVersionRequest.content,
            newTermVersionRequest.effectiveDate
        )
        publishTermUpdateMessage()
    }

    private fun publishTermUpdateMessage() {
        termAlertPort.sendAlert(
            AlertInformation(
                senderNickname = "ImHere",
                body = "지금 변경된 약관 내용을 확인해보세요",
                receiverEmail = null
            )
        )
    }
}
