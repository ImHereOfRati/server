package com.kdongsu5509.terms.controller

import com.kdongsu5509.support.external.DiscordUserErrorNotifier
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

@WebMvcTest(controllers = [TermsAdminController::class])
@AutoConfigureMockMvc(addFilters = false)
class TermsAdminControllerTest {

    companion object {
        const val TERM_ADMIN_CONTROLLER_URL = "/api/admin/terms"
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var termService: TermService

    @MockitoBean
    private lateinit var discordUserErrorNotifier: DiscordUserErrorNotifier

    @Test
    @DisplayName("모든 약관을 조회한다")
    fun readAll_success() {
        val results = listOf(
            TermResult(1L, 1L, TermTypes.SERVICE, "제목", "내용", LocalDateTime.now(), true)
        )
        BDDMockito.given(termService.findAll()).willReturn(results)

        mockMvc.perform(MockMvcRequestBuilders.get(TERM_ADMIN_CONTROLLER_URL))
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].id").value(1L))
    }
}
