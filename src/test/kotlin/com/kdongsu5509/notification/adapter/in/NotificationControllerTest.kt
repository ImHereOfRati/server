package com.kdongsu5509.imhere.notification.adapter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.kdongsu5509.imhere.TestFirebaseConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional // 👈 필수!

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestFirebaseConfig::class)
@Transactional
class NotificationControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var springDataUserRepository: SpringDataUserRepository

    @Autowired
    private lateinit var springDataFcmTokenRepository: SpringDataFcmTokenRepository

    @MockitoBean
    private lateinit var kakaoPublicKeyScheduler: KakaoPublicKeyScheduler

    @Test
    @WithMockUser(username = "dongsu@test.com")
    fun enroll_success() {
        // given
        saveDongsuInDBAsUser()

        val request = MyNotificationInfo(fcmToken = "test_token_123")

        // when & then
        mockMvc.perform(
            post("/api/v1/notification/enroll")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
    }


    @Test
    @WithMockUser(username = "dongsu@test.com")
    fun notify_self_success() {
        // given
        val user = saveDongsuInDBAsUser()

        springDataFcmTokenRepository.save(
            FcmTokenEntity(
                token = "existing_token_123",
                user = user
            )
        )

        // when & then
        mockMvc.perform(
            post("/api/v1/notification/self")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
    }

    private fun saveDongsuInDBAsUser(): UserJpaEntity {
        return springDataUserRepository.save(
            UserJpaEntity(
                email = "dongsu@test.com",
                role = UserRole.NORMAL,
                provider = OAuth2Provider.KAKAO
            )
        )
    }
}