package com.kdongsu5509.terms.adapter.`in`

import com.kdongsu5509.support.exception.type.UnprocessableEntityException
import com.kdongsu5509.support.external.DiscordUserErrorNotifier
import com.kdongsu5509.terms.TermException
import com.kdongsu5509.terms.adapter.`in`.dto.TermCreateRequest
import com.kdongsu5509.terms.application.TermResult
import com.kdongsu5509.terms.application.TermService
import com.kdongsu5509.terms.domain.TermTypes
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import java.time.LocalDateTime

@WebMvcTest(controllers = [TermsController::class])
@AutoConfigureMockMvc(addFilters = false)
class TermsControllerTest {

    companion object {
        const val TERM_CONTROLLER_URL = "/api/terms"
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
    @DisplayName("약관을 성공적으로 생성한다")
    fun create_success() {
        // given
        val request = TermCreateRequest(
            type = TermTypes.SERVICE,
            title = "제목",
            content = "내용",
            effectiveDate = LocalDateTime.now(),
            isRequired = true
        )
        val result = TermResult(1L, 1L, TermTypes.SERVICE, "제목", "내용", request.effectiveDate, true)
        given(termService.save(any())).willReturn(result)

        // when & then
        mockMvc.perform(
            post(TERM_CONTROLLER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(1L))
            .andExpect(jsonPath("$.data.title").value("제목"))
    }

    @Test
    @DisplayName("입력값이 올바르지 않으면 400 오류를 반환한다")
    fun create_fail_when_invalid_input() {
        // given
        val request = TermCreateRequest(
            type = TermTypes.SERVICE,
            title = "", // NotBlank 위반
            content = "  ", // NotBlank 위반
            effectiveDate = LocalDateTime.now(),
            isRequired = true
        )

        // when & then
        mockMvc.perform(
            post(TERM_CONTROLLER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.imhereResponseCode").value("GLOBAL-000"))
    }

    @Test
    @DisplayName("중복된 버전의 약관을 생성하려 하면 409 오류를 반환한다")
    fun create_fail_when_duplicate_version() {
        // given
        val request = TermCreateRequest(
            type = TermTypes.SERVICE,
            title = "제목",
            content = "내용",
            effectiveDate = LocalDateTime.now(),
            isRequired = true
        )
        given(termService.save(any())).willThrow(DataIntegrityViolationException("Duplicate version"))

        // when & then
        mockMvc.perform(
            post(TERM_CONTROLLER_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.imhereResponseCode").value("GLOBAL-500"))
    }

    @Test
    @DisplayName("모든 약관을 조회한다")
    fun readAll_success() {
        // given
        val results = listOf(
            TermResult(1L, 1L, TermTypes.SERVICE, "제목", "내용", LocalDateTime.now(), true)
        )
        given(termService.findAll()).willReturn(results)

        // when & then
        mockMvc.perform(get(TERM_CONTROLLER_URL))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(1L))
    }

    @Test
    @DisplayName("isActive 파라미터가 true이면 활성화된 약관을 조회한다")
    fun readAllByActive_success() {
        // given
        val results = listOf(
            TermResult(1L, 1L, TermTypes.SERVICE, "제목", "내용", LocalDateTime.now(), true)
        )
        given(termService.findAll(true)).willReturn(results)

        // when & then
        mockMvc.perform(
            get(TERM_CONTROLLER_URL)
                .param("isActive", "true")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].id").value(1L))
    }

    @Test
    @DisplayName("isActive 파라미터가 false이면 422 오류를 반환한다")
    fun readAllByActive_fail_when_inactive() {
        // given
        given(termService.findAll(false))
            .willThrow(
                UnprocessableEntityException(
                    message = TermException.NON_ACTIVE_TERM_NOT_ALLOWED.errorMessage
                )
            )

        // when & then
        mockMvc.perform(
            get(TERM_CONTROLLER_URL)
                .param("isActive", "false")
        )
            .andExpect(status().isUnprocessableContent)
            .andExpect(jsonPath("$.message").value("비활성화된 약관은 조회할 수 없습니다."))
    }
}
