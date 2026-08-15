package com.kdongsu5509.terms.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.security.shared.ImHereUserDetails
import com.kdongsu5509.terms.controller.dto.TermCreateRequest
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.repository.SpringDataTermRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.*
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.stream.Stream

private const val ADMIN_TERMS_PATH = "/api/admin/terms"

class TermsControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired
    private lateinit var termRepository: SpringDataTermRepository

    @BeforeEach
    fun cleanUp() {
        termRepository.deleteAll()
    }

    private val adminUser = ImHereUserDetails(
        email = "admin@example.com",
        nickname = "Admin",
        role = "ADMIN",
        status = "ACTIVE"
    )

    private val normalUser = ImHereUserDetails(
        email = "user@example.com",
        nickname = "User",
        role = "USER",
        status = "ACTIVE"
    )

    private fun errorResponseFields() = responseFields(
        fieldWithPath("imhereResponseCode").description("에러 코드"),
        fieldWithPath("message").description("에러 메시지"),
        fieldWithPath("data").description("없음").optional()
    )

    @Test
    @DisplayName("약관을 성공적으로 생성한다")
    fun createTermSuccess() {
        val request = TermCreateRequest(
            type = TermTypes.SERVICE,
            title = "서비스 이용약관",
            content = "이용약관 내용입니다.",
            effectiveDate = LocalDateTime.now().plusDays(1),
            isRequired = true
        )

        mockMvc.perform(
            post(ADMIN_TERMS_PATH)
                .with(csrf())
                .with(user(adminUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.title").value("서비스 이용약관"))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "terms-create-success",
                    snippets = arrayOf(
                        requestFields(
                            fieldWithPath("type").description("약관 타입 (SERVICE, PRIVACY, LOCATION)"),
                            fieldWithPath("title").description("약관 제목"),
                            fieldWithPath("content").description("약관 내용"),
                            fieldWithPath("effectiveDate").description("시행 일시 (ISO 8601)"),
                            fieldWithPath("isRequired").description("필수 동의 여부")
                        ),
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data.id").description("생성된 약관 ID"),
                            fieldWithPath("data.version").description("약관 버전"),
                            fieldWithPath("data.type").description("약관 타입"),
                            fieldWithPath("data.title").description("약관 제목"),
                            fieldWithPath("data.content").description("약관 내용"),
                            fieldWithPath("data.effectiveDate").description("시행 일시"),
                            fieldWithPath("data.isRequired").description("필수 여부")
                        )
                    )
                )
            )

        val terms = termRepository.findAll()
        assertThat(terms).hasSize(1)
        assertThat(terms[0].title).isEqualTo("서비스 이용약관")
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTermRequests")
    @DisplayName("약관 생성 요청의 필드 검증에 실패하면 400 오류를 반환한다")
    fun createTermFailWhenInvalid(
        caseName: String,
        request: TermCreateRequest,
        expectedMessage: String,
    ) {
        mockMvc.perform(
            post(ADMIN_TERMS_PATH)
                .with(csrf())
                .with(user(adminUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.imhereResponseCode").value("GLOBAL-000"))
            .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다: $expectedMessage"))

        assertThat(termRepository.count()).isZero()
    }

    @Test
    @DisplayName("활성화된 약관만 조회한다")
    fun readActiveTermsSuccess() {
        // given: 2개의 약관 생성
        val request1 = TermCreateRequest(
            type = TermTypes.SERVICE,
            title = "활성 약관",
            content = "내용",
            effectiveDate = LocalDateTime.now().minusDays(1), // 이미 시행됨 -> 활성화 상태
            isRequired = true
        )
        val request2 = TermCreateRequest(
            type = TermTypes.PRIVACY,
            title = "미시행 약관",
            content = "내용",
            effectiveDate = LocalDateTime.now().plusDays(10), // 미래 시행 -> 비활성화 상태
            isRequired = true
        )

        requestToAdminTermsPathWithPostMethod(request1)
        requestToAdminTermsPathWithPostMethod(request2)

        // when & then
        mockMvc.perform(
            get("/api/terms")
                .param("isActive", "true")
                .with(csrf())
                .with(user(normalUser))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].title").value("활성 약관"))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "terms-read-active-success",
                    snippets = arrayOf(
                        responseFields(
                            fieldWithPath("imhereResponseCode").description("응답 코드"),
                            fieldWithPath("message").description("응답 메시지"),
                            fieldWithPath("data[].id").description("약관 ID"),
                            fieldWithPath("data[].version").description("약관 버전"),
                            fieldWithPath("data[].type").description("약관 타입"),
                            fieldWithPath("data[].title").description("약관 제목"),
                            fieldWithPath("data[].content").description("약관 내용"),
                            fieldWithPath("data[].effectiveDate").description("시행 일시"),
                            fieldWithPath("data[].isRequired").description("필수 여부")
                        )
                    )
                )
            )
    }

    private fun requestToAdminTermsPathWithPostMethod(requestValue: TermCreateRequest) {
        mockMvc.perform(
            post(ADMIN_TERMS_PATH)
                .with(csrf())
                .with(user(adminUser))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(requestValue))
        ).andExpect(status().isOk)
    }

    companion object {
        private val validRequest = TermCreateRequest(
            type = TermTypes.SERVICE,
            title = "서비스 이용약관",
            content = "약관 내용",
            effectiveDate = LocalDateTime.of(2026, 7, 20, 12, 0),
            isRequired = true,
        )

        @JvmStatic
        fun invalidTermRequests(): Stream<Arguments> = Stream.of(
            invalidCase(
                "약관 종류 누락",
                validRequest.copy(type = null),
                "type: 약관 종류는 필수입니다.",
            ),
            invalidCase(
                "약관 제목 누락",
                validRequest.copy(title = null),
                "title: 약관 제목은 필수입니다.",
            ),
            invalidCase(
                "약관 제목 빈 문자열",
                validRequest.copy(title = ""),
                "title: 약관 제목은 필수입니다.",
            ),
            invalidCase(
                "약관 제목 공백",
                validRequest.copy(title = " \t"),
                "title: 약관 제목은 필수입니다.",
            ),
            invalidCase(
                "약관 내용 누락",
                validRequest.copy(content = null),
                "content: 약관 내용은 필수입니다.",
            ),
            invalidCase(
                "약관 내용 빈 문자열",
                validRequest.copy(content = ""),
                "content: 약관 내용은 필수입니다.",
            ),
            invalidCase(
                "약관 내용 공백",
                validRequest.copy(content = " \t"),
                "content: 약관 내용은 필수입니다.",
            ),
            invalidCase(
                "적용일 누락",
                validRequest.copy(effectiveDate = null),
                "effectiveDate: 적용일은 필수입니다.",
            ),
            invalidCase(
                "필수 여부 누락",
                validRequest.copy(isRequired = null),
                "isRequired: 필수 여부는 빈 값일 수 없습니다",
            ),
        )

        private fun invalidCase(
            caseName: String,
            request: TermCreateRequest,
            expectedMessage: String,
        ): Arguments = Arguments.of(caseName, request, expectedMessage)
    }
}
