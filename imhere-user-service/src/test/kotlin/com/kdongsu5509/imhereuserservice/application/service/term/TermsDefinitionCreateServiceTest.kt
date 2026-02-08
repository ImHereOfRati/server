package com.kdongsu5509.imhereuserservice.application.service.term

import com.kdongsu5509.imhereuserservice.adapter.`in`.web.terms.dto.NewTermDefinitionRequest
import com.kdongsu5509.imhereuserservice.application.port.out.term.TermsDefinitionLoadPort
import com.kdongsu5509.imhereuserservice.application.port.out.term.TermsDefinitionSavePort
import com.kdongsu5509.imhereuserservice.domain.terms.TermsTypes
import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class TermsDefinitionCreateServiceTest {

    @Mock
    lateinit var termsDefinitionLoadPort: TermsDefinitionLoadPort

    @Mock
    lateinit var termsDefinitionSavePort: TermsDefinitionSavePort

    private lateinit var termsDefinitionCreateService: TermsDefinitionCreateService

    @BeforeEach
    fun setUp() {
        termsDefinitionCreateService = TermsDefinitionCreateService(
            termsDefinitionLoadPort,
            termsDefinitionSavePort
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
            assertThat(it.errorCode).isEqualTo(ErrorCode.TERM_DEFINITION_ALREADY_EXIST)
        }
    }
}