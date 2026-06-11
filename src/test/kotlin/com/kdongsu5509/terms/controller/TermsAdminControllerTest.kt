package com.kdongsu5509.terms.controller

import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import com.kdongsu5509.terms.controller.dto.TermCreateRequest
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.service.TermResult
import com.kdongsu5509.terms.service.TermService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime

@WebMvcTest(controllers = [TermsAdminController::class])
@AutoConfigureMockMvc(addFilters = false)
class TermsAdminControllerTest {

    companion object {
        const val TERM_ADMIN_CONTROLLER_URL = "/api/admin/terms"
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jsonMapper: JsonMapper

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

    @Test
    @DisplayName("약관을 성공적으로 생성한다")
    fun create_success() {
        val request = TermCreateRequest(
            type = TermTypes.SERVICE,
            title = "제목",
            content = "내용",
            effectiveDate = LocalDateTime.now(),
            isRequired = true
        )
        val result = TermResult(1L, 1L, TermTypes.SERVICE, "제목", "내용", request.effectiveDate, true)
        BDDMockito.given(termService.save(any())).willReturn(result)

        mockMvc.perform(
            MockMvcRequestBuilders.post(TERM_ADMIN_CONTROLLER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.id").value(1L))
            .andExpect(MockMvcResultMatchers.jsonPath("$.data.title").value("제목"))
    }

    @Test
    @DisplayName("입력값이 올바르지 않으면 400 오류를 반환한다")
    fun create_fail_when_invalid_input() {
        val request = TermCreateRequest(
            type = TermTypes.SERVICE,
            title = "",
            content = "  ",
            effectiveDate = LocalDateTime.now(),
            isRequired = true
        )

        mockMvc.perform(
            MockMvcRequestBuilders.post(TERM_ADMIN_CONTROLLER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.imhereResponseCode").value("GLOBAL-000"))
    }

    @Test
    @DisplayName("중복된 버전의 약관을 생성하려 하면 409 오류를 반환한다")
    fun create_fail_when_duplicate_version() {
        val request = TermCreateRequest(
            type = TermTypes.SERVICE,
            title = "제목",
            content = "내용",
            effectiveDate = LocalDateTime.now(),
            isRequired = true
        )
        BDDMockito.given(termService.save(any())).willThrow(DataIntegrityViolationException("Duplicate version"))

        mockMvc.perform(
            MockMvcRequestBuilders.post(TERM_ADMIN_CONTROLLER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(MockMvcResultMatchers.status().isConflict)
            .andExpect(MockMvcResultMatchers.jsonPath("$.imhereResponseCode").value("GLOBAL-500"))
    }
}
