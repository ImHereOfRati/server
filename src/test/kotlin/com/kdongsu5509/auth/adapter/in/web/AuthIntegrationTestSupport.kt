package com.kdongsu5509.auth.adapter.`in`.web

import com.common.testsupport.PersistenceTestSupport
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.filter.CharacterEncodingFilter
import tools.jackson.databind.json.JsonMapper

/**
 * Auth 도메인 Controller 통합 테스트를 위한 기반 클래스.
 *
 * - @SpringBootTest: 실제 Spring 컨텍스트를 전부 띄워 Security 필터 체인까지 검증한다.
 * - springSecurity() 적용: 실제 인증/인가 흐름이 동작한다.
 * - RestDocumentation 설정: MockMvc에 RestDocs 컨텍스트를 주입해 epages 문서 자동 생성을 지원한다.
 */
@SpringBootTest
@ExtendWith(RestDocumentationExtension::class)
abstract class AuthIntegrationTestSupport : PersistenceTestSupport() {

    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var jsonMapper: JsonMapper

    @BeforeEach
    fun setUpMockMvc(
        webApplicationContext: WebApplicationContext,
        restDocumentation: RestDocumentationContextProvider
    ) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
            .alwaysDo<DefaultMockMvcBuilder>(MockMvcResultHandlers.print())
            .addFilters<DefaultMockMvcBuilder>(CharacterEncodingFilter("UTF-8", true))
            .build()
    }
}
