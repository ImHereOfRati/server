package com.kdongsu5509.user.adapter.`in`.web.terms

import com.common.testUtil.ControllerTestSupport
import com.kdongsu5509.user.adapter.`in`.web.terms.dto.NewTermDefinitionRequest
import com.kdongsu5509.user.adapter.`in`.web.terms.dto.NewTermVersionRequest
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.SpringDataTermsDefinitionRepository
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.SpringDataTermsVersionRepository
import com.kdongsu5509.user.domain.terms.TermsTypes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

class TermsAdminControllerTest : ControllerTestSupport() {

    companion object {
        const val BASE_URL = "/api/admin/terms"
        const val DEFINITION_URL = "/definition"
        const val VERSION_URL = "/version"
    }

    @Autowired
    lateinit var springDataTermsDefinitionRepository: SpringDataTermsDefinitionRepository

    @Autowired
    lateinit var springDataTermsVersionRepository: SpringDataTermsVersionRepository

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("관리자 권한으로 새로운 약관 종류를 생성한다.")
    fun createNewTermDefinition_Success() {
        // given
        val request = NewTermDefinitionRequest("위치 정보 이용 약관", TermsTypes.LOCATION, true)

        // when & then
        mockMvc.perform(
            post(BASE_URL + DEFINITION_URL)
                .with(csrf()) // Spring Security 사용 시 필요
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isOk)
    }

    @Test
    @WithMockUser(roles = ["USER"])
    @DisplayName("관리자 권한이 없으면 약관 생성에 실패한다(403).")
    fun createNewTermDefinition_Forbidden() {
        // given
        val request = NewTermDefinitionRequest("잘못된 접근", TermsTypes.SERVICE, true)

        // when & then
        mockMvc.perform(
            post(BASE_URL + DEFINITION_URL)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isForbidden)
    }

    @Test
    @WithMockUser(roles = ["ADMIN"])
    @DisplayName("약관 종류와 세부 내용을 순차적으로 생성하고 DB 저장까지 확인한다")
    fun create_Definition_And_Version_Integration_Success() {
        // 1. 약관 종류(Definition) 생성
        val defRequest = NewTermDefinitionRequest("통합 테스트 약관", TermsTypes.SERVICE, true)

        mockMvc.perform(
            post(BASE_URL + DEFINITION_URL)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(defRequest))
        ).andExpect(status().isOk)

        // DB에 잘 들어갔는지 확인
        val savedDef = springDataTermsDefinitionRepository.findAll()
            .find { it.termsTitle == "통합 테스트 약관" }
        assertThat(savedDef).isNotNull

        // 2. 약관 세부 내용(Version) 생성
        val verRequest = NewTermVersionRequest(
            termDefinitionId = savedDef!!.id!!,
            version = "v1.0",
            content = "테스트 내용",
            effectiveDate = LocalDateTime.now().plusDays(1)
        )

        mockMvc.perform(
            post(BASE_URL + VERSION_URL)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(verRequest))
        ).andExpect(status().isOk)

        val savedVer = springDataTermsVersionRepository.findActiveVersion(savedDef.id!!).get()
        assertThat(savedVer).isNotNull
        assertThat(savedVer.version).isEqualTo("v1.0")
    }
}
