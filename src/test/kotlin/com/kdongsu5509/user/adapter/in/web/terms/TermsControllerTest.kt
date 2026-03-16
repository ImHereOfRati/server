package com.kdongsu5509.user.adapter.`in`.web.terms

import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.SpringDataTermsDefinitionRepository
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.SpringDataTermsVersionRepository
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsDefinitionJpaEntity
import com.kdongsu5509.user.adapter.out.persistence.terms.jpa.TermsVersionJpaEntity
import com.kdongsu5509.user.domain.terms.TermsTypes
import com.kdongsu5509.user.testSupport.ControllerTestSupport
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.restdocs.request.RequestDocumentation.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@ActiveProfiles("test")
class TermsControllerTest @Autowired constructor(
    private val springDataTermsDefinitionRepository: SpringDataTermsDefinitionRepository,
    private val springDataTermsVersionRepository: SpringDataTermsVersionRepository
) : ControllerTestSupport() {

    @Test
    @DisplayName("약관 목록 조회 시 페이징된 결과를 반환한다")
    fun readAllTermsDefinitions_Success() {
        // given: 테스트용 데이터 2개 저장
        springDataTermsDefinitionRepository.saveAll(
            listOf(
                TermsDefinitionJpaEntity("서비스 이용약관", TermsTypes.SERVICE, true),
                TermsDefinitionJpaEntity("개인정보 처리방침", TermsTypes.PRIVACY, true)
            )
        )

        // when & then
        mockMvc.perform(
            get("/api/v1/user/terms")
                .param("page", "0")
                .param("size", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content.length()").value(2))
            .andExpect(jsonPath("$.data.content[0].title").exists())
            .andDo(
                document(
                    "terms-get-all",
                    queryParameters(
                        parameterWithName("page").description("페이지 번호 (0부터 시작)"),
                        parameterWithName("size").description("페이지 크기")
                    ),
                    responseFields(
                        fieldWithPath("code").description("응답 코드"),
                        fieldWithPath("message").description("응답 메시지").optional(),
                        fieldWithPath("data.content[].termDefinitionId").description("약관 정의 ID"),
                        fieldWithPath("data.content[].title").description("약관 제목"),
                        fieldWithPath("data.content[].termsTypes").description("약관 타입 (SERVICE, PRIVACY, LOCATION, MARKETING)"),
                        fieldWithPath("data.content[].isRequired").description("필수 여부"),
                        fieldWithPath("data.pageable.pageNumber").description("현재 페이지 번호"),
                        fieldWithPath("data.pageable.pageSize").description("페이지 크기"),
                        fieldWithPath("data.pageable.sort.empty").description("정렬 정보 없음 여부"),
                        fieldWithPath("data.pageable.sort.sorted").description("정렬 여부"),
                        fieldWithPath("data.pageable.sort.unsorted").description("정렬 안됨 여부"),
                        fieldWithPath("data.pageable.offset").description("오프셋"),
                        fieldWithPath("data.pageable.paged").description("페이징 여부"),
                        fieldWithPath("data.pageable.unpaged").description("페이징 안됨 여부"),
                        fieldWithPath("data.last").description("마지막 페이지 여부"),
                        fieldWithPath("data.totalPages").description("전체 페이지 수"),
                        fieldWithPath("data.totalElements").description("전체 요소 수"),
                        fieldWithPath("data.size").description("페이지 크기"),
                        fieldWithPath("data.number").description("현재 페이지 번호"),
                        fieldWithPath("data.sort.empty").description("정렬 정보 없음 여부"),
                        fieldWithPath("data.sort.sorted").description("정렬 여부"),
                        fieldWithPath("data.sort.unsorted").description("정렬 안됨 여부"),
                        fieldWithPath("data.first").description("첫 페이지 여부"),
                        fieldWithPath("data.numberOfElements").description("현재 페이지 요소 수"),
                        fieldWithPath("data.empty").description("비어 있음 여부")
                    )
                )
            )
    }

    @Test
    @DisplayName("약관 버전 상세 조회 시 활성화된 버전을 반환한다")
    fun readTermsVersion_Success() {
        // given
        val definition = springDataTermsDefinitionRepository.save(
            TermsDefinitionJpaEntity("위치 정보 약관", TermsTypes.LOCATION, true)
        )
        springDataTermsVersionRepository.save(
            TermsVersionJpaEntity("v1.0", "약관 내용", true, LocalDateTime.now(), definition)
        )

        // when & then
        mockMvc.perform(
            get("/api/v1/user/terms/version/{termsDefinitionId}", definition.id)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.version").value("v1.0"))
            .andExpect(jsonPath("$.data.content").value("약관 내용"))
            .andDo(
                document(
                    "terms-version-get",
                    pathParameters(
                        parameterWithName("termsDefinitionId").description("약관 정의 ID")
                    ),
                    responseFields(
                        fieldWithPath("code").description("응답 코드"),
                        fieldWithPath("message").description("응답 메시지").optional(),
                        fieldWithPath("data.version").description("약관 버전"),
                        fieldWithPath("data.content").description("약관 내용"),
                        fieldWithPath("data.effectiveDate").description("발효 일시")
                    )
                )
            )
    }

    @Test
    @DisplayName("존재하지 않는 약관 ID로 버전 조회 시 에러를 반환한다")
    fun readTermsVersion_Fail_NotFound() {
        // when & then
        mockMvc.perform(
            get("/api/v1/user/terms/version/{termsDefinitionId}", 9999)
        )
            .andExpect(status().isNotFound)
            .andDo(
                document(
                    "terms-version-get-not-found",
                    pathParameters(
                        parameterWithName("termsDefinitionId").description("존재하지 않는 약관 정의 ID")
                    ),
                    responseFields(
                        fieldWithPath("code").description("응답 상태 코드"),
                        fieldWithPath("message").description("에러 메시지"),
                        fieldWithPath("data.code").description("비즈니스 에러 코드").optional(),
                        fieldWithPath("data.message").description("비즈니스 에러 메시지").optional()
                    )
                )
            )
    }
}