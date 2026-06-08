package com.kdongsu5509.notifications.adapter.`in`.web

import com.common.testsupport.WebIntegrationTestSupport
import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import com.kdongsu5509.auth.security.ImHereUserDetails
import com.kdongsu5509.notifications.adapter.`in`.web.dto.FcmTokenEnrollRequest
import com.kdongsu5509.notifications.adapter.out.persistence.FcmTokenJpaEntity
import com.kdongsu5509.notifications.adapter.out.persistence.SpringDataFcmTokenRepository
import com.kdongsu5509.notifications.domain.DeviceType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class FcmTokenEnrollControllerIntegrationTest : WebIntegrationTestSupport() {

    @Autowired
    private lateinit var fcmTokenRepository: SpringDataFcmTokenRepository

    private val userDetails = ImHereUserDetails(
        email = "test@example.com",
        nickname = "tester",
        role = "USER",
        status = "ACTIVE"
    )

    @Test
    @DisplayName("FCM 토큰을 새로 저장하면 201 Created를 반환한다")
    fun enrollSuccess() {
        val request = FcmTokenEnrollRequest(
            fcmToken = "token-1",
            deviceType = DeviceType.IOS
        )

        mockMvc.perform(
            post("/api/fcm-tokens")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isCreated)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-fcm-token-enroll-success",
                    snippets = arrayOf(
                        requestFields(
                            fieldWithPath("fcmToken").description("등록할 FCM 토큰"),
                            fieldWithPath("deviceType").description("디바이스 타입").type(JsonFieldType.STRING)
                        )
                    )
                )
            )

        val saved = fcmTokenRepository.findByEmail(userDetails.email)
        assertThat(saved).isNotNull
        assertThat(saved!!.token).isEqualTo("token-1")
        assertThat(saved.deviceType).isEqualTo(DeviceType.IOS)
    }

    @Test
    @DisplayName("FCM 토큰이 이미 있으면 기존 값을 갱신한다")
    fun enrollUpdatesExistingToken() {
        fcmTokenRepository.save(
            FcmTokenJpaEntity(
                token = "old-token",
                email = userDetails.email,
                deviceType = DeviceType.AOS
            )
        )

        val request = FcmTokenEnrollRequest(
            fcmToken = "new-token",
            deviceType = DeviceType.IOS
        )

        mockMvc.perform(
            post("/api/fcm-tokens")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isCreated)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-fcm-token-enroll-update-success",
                    snippets = arrayOf(
                        requestFields(
                            fieldWithPath("fcmToken").description("갱신할 FCM 토큰"),
                            fieldWithPath("deviceType").description("디바이스 타입").type(JsonFieldType.STRING)
                        )
                    )
                )
            )

        val saved = fcmTokenRepository.findByEmail(userDetails.email)
        assertThat(saved).isNotNull
        assertThat(saved!!.token).isEqualTo("new-token")
        assertThat(saved.deviceType).isEqualTo(DeviceType.AOS)
    }

    @Test
    @DisplayName("FCM 토큰이 비어 있으면 400 Bad Request를 반환한다")
    fun enrollFailsWhenTokenIsBlank() {
        val request = FcmTokenEnrollRequest(
            fcmToken = "",
            deviceType = DeviceType.IOS
        )

        mockMvc.perform(
            post("/api/fcm-tokens")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isBadRequest)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-fcm-token-enroll-fail-blank-token",
                    snippets = arrayOf(
                        requestFields(
                            fieldWithPath("fcmToken").description("등록할 FCM 토큰"),
                            fieldWithPath("deviceType").description("디바이스 타입").type(JsonFieldType.STRING)
                        )
                    )
                )
            )
    }

    @Test
    @DisplayName("디바이스 타입이 없으면 400 Bad Request를 반환한다")
    fun enrollFailsWhenDeviceTypeMissing() {
        val requestJson = """
            {
              "fcmToken": "token-1"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/fcm-tokens")
                .with(csrf())
                .with(user(userDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson)
        ).andExpect(status().isBadRequest)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    identifier = "notifications-fcm-token-enroll-fail-missing-device-type",
                    snippets = arrayOf(
                        requestFields(
                            fieldWithPath("fcmToken").description("등록할 FCM 토큰"),
                            fieldWithPath("deviceType").description("디바이스 타입").type(JsonFieldType.STRING).optional()
                        )
                    )
                )
            )
    }
}
