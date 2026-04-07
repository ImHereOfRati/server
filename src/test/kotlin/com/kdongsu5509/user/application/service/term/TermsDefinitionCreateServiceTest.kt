package com.kdongsu5509.user.application.service.term

import com.kdongsu5509.support.exception.BusinessException
import com.kdongsu5509.support.exception.TermErrorCode
import com.kdongsu5509.user.adapter.`in`.web.terms.dto.NewTermDefinitionRequest
import com.kdongsu5509.user.application.dto.AlertInformation
import com.kdongsu5509.user.application.port.out.noti.TermAlertPort
import com.kdongsu5509.user.application.port.out.term.TermsDefinitionLoadPort
import com.kdongsu5509.user.application.port.out.term.TermsDefinitionSavePort
import com.kdongsu5509.user.domain.terms.TermsTypes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.then
import org.mockito.BDDMockito.willDoNothing
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.times

@ExtendWith(MockitoExtension::class)
class TermsDefinitionCreateServiceTest {

    @Mock
    lateinit var termsDefinitionLoadPort: TermsDefinitionLoadPort

    @Mock
    lateinit var termsDefinitionSavePort: TermsDefinitionSavePort

    @Mock
    lateinit var termAlertPort: TermAlertPort

    private lateinit var termsDefinitionCreateService: TermsDefinitionCreateService

    @BeforeEach
    fun setUp() {
        termsDefinitionCreateService = TermsDefinitionCreateService(
            termsDefinitionLoadPort,
            termsDefinitionSavePort,
            termAlertPort
        )
    }

    @Test
    @DisplayName("새로운 약관 정의를 성공적으로 생성한다.")
    fun createNewTermsDefinition_Success() {
        // given
        val termsName = "위치정보 활용동의"
        val termsType = TermsTypes.LOCATION
        val required = true

        val testReq = NewTermDefinitionRequest(termsName, termsType, required)

        `when`(termsDefinitionLoadPort.checkExistence(termsName, termsType)).thenReturn(false)

        // when
        termsDefinitionCreateService.createNewTermsDefinition(testReq)

        // then
        verify(termsDefinitionLoadPort).checkExistence(termsName, termsType)
        verify(termsDefinitionSavePort).saveTermDefinition(termsName, termsType, required)
    }

    @Test
    @DisplayName("이미 존재하는 약관 이름과 타입인 경우 예외가 발생한다.")
    fun createNewTermsDefinition_AlreadyExist() {
        // given
        val termsName = "서비스 이용약관"
        val termsType = TermsTypes.SERVICE
        val required = true
        val testReq = NewTermDefinitionRequest(termsName, termsType, required)

        `when`(termsDefinitionLoadPort.checkExistence(termsName, termsType)).thenReturn(true)

        // when & then
        assertThrows<BusinessException> {
            termsDefinitionCreateService.createNewTermsDefinition(testReq)
        }.also {
            assertThat(it.errorCode).isEqualTo(TermErrorCode.TERM_DEFINITION_ALREADY_EXIST)
        }
    }

    @Test
    @DisplayName("새로운 약관 정의가 성공적으로 생성되면, 최종적으로 MQ 메시지를 발행한다.")
    fun createNewTermsDefinition_andThenPublishMessage_Success() {
        // given
        val termsName = "위치정보 활용동의"
        val termsType = TermsTypes.LOCATION
        val required = true

        val testReq = NewTermDefinitionRequest(termsName, termsType, required)
        val alertInformation = AlertInformation(
            senderNickname = "ImHere",
            body = "새로운 약관 $termsName 이 추가되었습니다",
            receiverEmail = null
        )

        willDoNothing().given(termAlertPort).sendAlert(alertInformation)

        `when`(termsDefinitionLoadPort.checkExistence(termsName, termsType)).thenReturn(false)

        // when
        termsDefinitionCreateService.createNewTermsDefinition(testReq)

        // then
        then(termAlertPort).should(times(1)).sendAlert(alertInformation)
    }
}
