package com.kdongsu5509.imhereuserservice.application.service.term

import com.kdongsu5509.imhereuserservice.application.port.out.term.TermsVersionLoadPort
import com.kdongsu5509.imhereuserservice.domain.terms.TermVersion
import com.kdongsu5509.imhereuserservice.support.exception.BusinessException
import com.kdongsu5509.imhereuserservice.support.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import java.time.LocalDateTime
import kotlin.test.Test

@ExtendWith(MockitoExtension::class)
class TermsVersionReadServiceTest {

    @Mock
    lateinit var termsVersionLoadPort: TermsVersionLoadPort

    @InjectMocks
    lateinit var termsVersionReadService: TermsVersionReadService

    @Test
    @DisplayName("약관 ID로 조회 시 해당 버전 정보를 Response DTO로 변환하여 반환한다")
    fun read_success() {
        // given
        val termDefinitionId = 1L
        val domainEntity = TermVersion(
            termDefinitionId = 10L,
            version = "v1.0",
            content = "테스트 약관 내용",
            effectiveDate = LocalDateTime.now(),
        )

        given(termsVersionLoadPort.loadSpecificActiveTermVersion(termDefinitionId))
            .willReturn(domainEntity)

        // when
        val result = termsVersionReadService.read(termDefinitionId)

        // then
        assertThat(result.version).isEqualTo("v1.0")
        assertThat(result.content).isEqualTo("테스트 약관 내용")

        verify(termsVersionLoadPort).loadSpecificActiveTermVersion(termDefinitionId)
    }

    @Test
    @DisplayName("Port에서 예외가 발생하면 서비스 계층에서도 그대로 전파된다")
    fun read_fail_notFound() {
        // given
        val invalidId = 999L
        given(termsVersionLoadPort.loadSpecificActiveTermVersion(invalidId))
            .willThrow(
                BusinessException(
                    ErrorCode.TERM_DEFINITION_NOT_FOUND
                )
            )

        // when & then
        assertThatThrownBy {
            termsVersionReadService.read(invalidId)
        }.isInstanceOf(BusinessException::class.java)
    }
}