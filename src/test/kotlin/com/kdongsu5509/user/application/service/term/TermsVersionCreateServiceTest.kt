package com.kdongsu5509.user.application.service.term

import com.kdongsu5509.user.adapter.`in`.web.terms.dto.NewTermVersionRequest
import com.kdongsu5509.user.application.dto.AlertInformation
import com.kdongsu5509.user.application.port.out.noti.TermAlertPort
import com.kdongsu5509.user.application.port.out.term.TermsVersionSavePort
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.willDoNothing
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.then
import org.mockito.kotlin.times
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
class TermsVersionCreateServiceTest {

    @Mock
    lateinit var termsVersionSavePort: TermsVersionSavePort

    @Mock
    lateinit var termAlertPort: TermAlertPort

    @InjectMocks
    lateinit var termsVersionCreateService: TermsVersionCreateService

    @Test
    @DisplayName("약관 버전 생성 요청을 받으면 SavePort를 호출하여 저장한다")
    fun createNewTermVersion_success() {
        // given
        val request = NewTermVersionRequest(
            termDefinitionId = 1L,
            version = "v1.0",
            content = "약관 내용입니다.",
            effectiveDate = LocalDateTime.now()
        )

        // when
        termsVersionCreateService.createNewTermVersion(request)

        // then
        // 서비스가 받은 인자들을 Port에 그대로 잘 전달했는지 확인
        verify(termsVersionSavePort).saveTermVersion(
            request.termDefinitionId,
            request.version,
            request.content,
            request.effectiveDate
        )
    }

    @Test
    @DisplayName("약관 버전 생성 요청을 받으면 SavePort를 호출하여 저장한다")
    fun createNewTermVersion_andThenPublishMessage_success() {
        // given
        val request = NewTermVersionRequest(
            termDefinitionId = 1L,
            version = "v1.0",
            content = "약관 내용입니다.",
            effectiveDate = LocalDateTime.now()
        )

        val alertInformation = AlertInformation(
            senderNickname = "ImHere",
            body = "지금 변경된 약관 내용을 확인해보세요",
            receiverEmail = null
        )

        willDoNothing().given(termAlertPort).sendAlert(alertInformation)

        // when
        termsVersionCreateService.createNewTermVersion(request)

        // then
        then(termAlertPort).should(times(1)).sendAlert(alertInformation)
    }
}
