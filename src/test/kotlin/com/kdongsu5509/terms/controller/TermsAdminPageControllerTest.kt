package com.kdongsu5509.terms.controller

import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.service.TermResult
import com.kdongsu5509.terms.service.TermService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.view
import java.time.LocalDateTime

@WebMvcTest(controllers = [TermsAdminPageController::class])
@AutoConfigureMockMvc(addFilters = false)
class TermsAdminPageControllerTest {

    companion object {
        const val TERMS_ADMIN_PAGE_URL = "/admin/terms"
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var termService: TermService

    @MockitoBean
    private lateinit var discordUserErrorNotifier: DiscordUserErrorNotifier

    @Test
    @DisplayName("약관 관리 페이지는 전체 약관을 모델에 담아 admin/terms 뷰를 반환한다")
    fun page_success() {
        val results = listOf(
            TermResult(1L, 1L, TermTypes.SERVICE, "서비스 이용약관", "내용", LocalDateTime.now(), true),
            TermResult(2L, 1L, TermTypes.PRIVACY, "개인정보 처리방침", "내용", LocalDateTime.now(), false),
        )
        given(termService.findAll()).willReturn(results)

        mockMvc.perform(get(TERMS_ADMIN_PAGE_URL).with(csrf()))
            .andExpect(status().isOk)
            .andExpect(view().name("admin/terms"))
            .andExpect(model().attribute("terms", results))
    }

    @Test
    @DisplayName("약관이 하나도 없어도 빈 목록으로 admin/terms 뷰를 반환한다")
    fun page_success_when_no_terms() {
        given(termService.findAll()).willReturn(emptyList())

        mockMvc.perform(get(TERMS_ADMIN_PAGE_URL).with(csrf()))
            .andExpect(status().isOk)
            .andExpect(view().name("admin/terms"))
            .andExpect(model().attribute("terms", emptyList<TermResult>()))
    }
}
