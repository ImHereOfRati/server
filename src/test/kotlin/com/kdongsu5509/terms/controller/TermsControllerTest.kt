package com.kdongsu5509.terms.controller

import com.kdongsu5509.support.exception.type.UnprocessableEntityException
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.service.TermResult
import com.kdongsu5509.terms.service.TermService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.LocalDateTime

@WebMvcTest(controllers = [TermsController::class])
@AutoConfigureMockMvc(addFilters = false)
class TermsControllerTest {

    companion object {
        const val TERM_CONTROLLER_URL = "/api/terms"
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var termService: TermService

    @MockitoBean
    private lateinit var discordUserErrorNotifier: DiscordUserErrorNotifier

    @Test
    @DisplayName("isActive 파라미터가 true이면 활성화된 약관을 조회한다")
    fun readAllByActive_success() {
        // given
        val results = listOf(
            TermResult(1L, 1L, TermTypes.SERVICE, "제목", "내용", LocalDateTime.now(), true)
        )
        BDDMockito.given(termService.findAll(true)).willReturn(results)

        // when & then
        mockMvc.perform(
            MockMvcRequestBuilders.get(TERM_CONTROLLER_URL)
                .param("isActive", "true")
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].id").value(1L))
    }

    @Test
    @DisplayName("isActive 파라미터가 false이면 422 오류를 반환한다")
    fun readAllByActive_fail_when_inactive() {
        // given
        BDDMockito.given(termService.findAll(false))
            .willThrow(
                UnprocessableEntityException(
                    message = TermException.NON_ACTIVE_TERM_NOT_ALLOWED.errorMessage
                )
            )

        // when & then
        mockMvc.perform(
            MockMvcRequestBuilders.get(TERM_CONTROLLER_URL)
                .param("isActive", "false")
        )
            .andExpect(MockMvcResultMatchers.status().isUnprocessableContent)
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("비활성화된 약관은 조회할 수 없습니다."))
    }
}
