package com.common.testUtil

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.kdongsu5509.support.config.QueryDslConfig
import com.kdongsu5509.support.config.RabbitMQConfig
import com.kdongsu5509.user.adapter.out.persistence.user.jpa.SpringQueryDSLUserRepository
import com.kdongsu5509.user.application.port.out.noti.FriendAlertPort
import com.kdongsu5509.user.application.port.out.noti.TermAlertPort
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.restdocs.RestDocumentationContextProvider
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.filter.CharacterEncodingFilter
import tools.jackson.databind.json.JsonMapper

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@Import(
    SpringQueryDSLUserRepository::class,
    QueryDslConfig::class,
    RabbitMQConfig::class
)
//@Import(RestDocsConfiguration::class)
@ExtendWith(RestDocumentationExtension::class)
abstract class ControllerTestSupport : TestRedisContainer() {

    @MockitoBean
    protected lateinit var friendAlertPort: FriendAlertPort

    @MockitoBean
    protected lateinit var termAlertPort: TermAlertPort

    @MockitoBean
    protected lateinit var firebaseMessaging: FirebaseMessaging

    @MockitoBean
    protected lateinit var firebaseApp: FirebaseApp

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var jsonMapper: JsonMapper

    @Autowired
    @Qualifier("customRedisTemplate")
    protected lateinit var redisTemplate: org.springframework.data.redis.core.RedisTemplate<String, Any>

//    @Autowired
//    protected lateinit var restDocs: RestDocumentationResultHandler

    @BeforeEach
    fun setUp(
        webApplicationContext: WebApplicationContext,
        restDocumentationContextProvider: RestDocumentationContextProvider
    ) {
        redisTemplate.execute { connection ->
            connection.serverCommands().flushDb()
            null
        }

        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
//            .apply<DefaultMockMvcBuilder>(
//                MockMvcRestDocumentation.documentationConfiguration(restDocumentationContextProvider)
//            )
            .apply<DefaultMockMvcBuilder>(springSecurity())
            .alwaysDo<DefaultMockMvcBuilder>(MockMvcResultHandlers.print())
//            .alwaysDo<DefaultMockMvcBuilder>(restDocs)
            .addFilters<DefaultMockMvcBuilder>(CharacterEncodingFilter("UTF-8", true))
            .build()
    }
}
