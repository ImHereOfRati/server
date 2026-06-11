package com.kdongsu5509.terms.controller

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.terms.controller.dto.TermCreateRequest
import com.kdongsu5509.terms.domain.TermTypes
import com.kdongsu5509.terms.repository.SpringDataTermRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

class TermsAdminControllerIntegrationTest : WebIntegrationTestSupport() {

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

    @Test
    @DisplayName("전체 약관 조회는 ADMIN 권한이 있어야 성공한다")
    fun readAllTermsAdminSuccess() {
        val request = TermCreateRequest(
            type = TermTypes.LOCATION,
            title = "위치 기반 약관",
            content = "내용",
            effectiveDate = LocalDateTime.now(),
            isRequired = true
        )

        mockMvc.perform(
            post("/api/terms").with(csrf()).with(user(adminUser)).contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isOk)

        mockMvc.perform(
            get("/api/admin/terms")
                .with(csrf())
                .with(user(adminUser))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].title").value("위치 기반 약관"))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "terms-read-all-admin-success",
                    snippets = arrayOf(
                        relaxedResponseFields(
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

    @Test
    @DisplayName("전체 약관 조회 시 USER 권한이면 403 Forbidden 발생")
    fun readAllTermsUserForbidden() {
        mockMvc.perform(
            get("/api/admin/terms")
                .with(csrf())
                .with(user(normalUser))
        )
            .andExpect(status().isForbidden)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "terms-read-all-forbidden"
                )
            )
    }
}
