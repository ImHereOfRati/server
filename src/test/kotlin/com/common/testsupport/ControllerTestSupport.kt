//package com.common.testsupport
//
//import com.google.firebase.FirebaseApp
//import com.google.firebase.messaging.FirebaseMessaging
//import com.kdongsu5509.support.config.HttpExchangeConfig
//import com.kdongsu5509.support.config.QueryDslConfig
//import com.kdongsu5509.support.config.RabbitMQConfig
//import com.kdongsu5509.user.repository.jpa.SpringQueryDSLUserRepository
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.extension.ExtendWith
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.beans.factory.annotation.Qualifier
//import org.springframework.boot.test.context.SpringBootTest
//import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
//import org.springframework.context.annotation.Import
//import org.springframework.restdocs.RestDocumentationContextProvider
//import org.springframework.restdocs.RestDocumentationExtension
//import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
//import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
//import org.springframework.test.context.ActiveProfiles
//import org.springframework.test.context.DynamicPropertyRegistry
//import org.springframework.test.context.DynamicPropertySource
//import org.springframework.test.context.bean.override.mockito.MockitoBean
//import org.springframework.test.web.servlet.MockMvc
//import org.springframework.test.web.servlet.result.MockMvcResultHandlers
//import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
//import org.springframework.test.web.servlet.setup.MockMvcBuilders
//import org.springframework.transaction.annotation.Transactional
//import org.springframework.web.context.WebApplicationContext
//import org.springframework.web.filter.CharacterEncodingFilter
//import tools.jackson.databind.json.JsonMapper
//
//@SpringBootTest
//@Transactional
//@ActiveProfiles("test")
//@AutoConfigureMockMvc
//@Import(
//    SpringQueryDSLUserRepository::class,
//    QueryDslConfig::class,
//    RabbitMQConfig::class,
//    HttpExchangeConfig::class
//)
//@ExtendWith(RestDocumentationExtension::class)
//abstract class ControllerTestSupport : TestRedisContainer() {
//
//    companion object {
//        @JvmStatic
//        @DynamicPropertySource
//        fun configureRabbitMqProperties(registry: DynamicPropertyRegistry) {
//            registry.add("spring.rabbitmq.host") { TestRabbitMQContainer.rabbitMqContainer.host }
//            registry.add("spring.rabbitmq.port") { TestRabbitMQContainer.rabbitMqContainer.getMappedPort(5672) }
//            registry.add("spring.rabbitmq.virtual-host") { "/" }
//            registry.add("spring.rabbitmq.username") { "guest" }
//            registry.add("spring.rabbitmq.password") { "guest" }
//        }
//    }
//
//
//    @MockitoBean
//    protected lateinit var friendAlertPort: FriendAlertPort
//
//    @MockitoBean
//    protected lateinit var termAlertPort: TermAlertPort
//
//    @MockitoBean
//    protected lateinit var firebaseMessaging: FirebaseMessaging
//
//    @MockitoBean
//    protected lateinit var firebaseApp: FirebaseApp
//
//    @Autowired
//    protected lateinit var mockMvc: MockMvc
//
//    @Autowired
//    protected lateinit var jsonMapper: JsonMapper
//
//    @Autowired
//    @Qualifier("customRedisTemplate")
//    protected lateinit var redisTemplate: org.springframework.data.redis.core.RedisTemplate<String, Any>
//
//    @BeforeEach
//    fun setUp(
//        webApplicationContext: WebApplicationContext,
//        restDocumentation: RestDocumentationContextProvider
//    ) {
//        redisTemplate.execute { connection ->
//            connection.serverCommands().flushDb()
//            null
//        }
//
//        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
//            .apply<DefaultMockMvcBuilder>(springSecurity())
//            .apply<DefaultMockMvcBuilder>(MockMvcRestDocumentation.documentationConfiguration(restDocumentation))
//            .alwaysDo<DefaultMockMvcBuilder>(MockMvcResultHandlers.print())
//            .addFilters<DefaultMockMvcBuilder>(CharacterEncodingFilter("UTF-8", true))
//            .build()
//    }
//}
