package com.kdongsu5509.imhere.message.adapter.`in`

import com.fasterxml.jackson.databind.ObjectMapper
import com.kdongsu5509.imhere.common.alert.port.out.MessageSendPort
import com.kdongsu5509.imhere.message.adapter.dto.MessageSendRequest
import com.kdongsu5509.imhere.message.application.port.MultipleMessageSendUseCasePort
import com.kdongsu5509.imhere.message.application.port.SingleMessageSendUseCasePort
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.filter.OncePerRequestFilter
import kotlin.test.Test

@WebMvcTest(
    controllers = [MessageController::class],
    excludeAutoConfiguration = [
        DataSourceAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,
        JpaRepositoriesAutoConfiguration::class,
        UserDetailsServiceAutoConfiguration::class
    ],
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [OncePerRequestFilter::class]
        )
    ]
)
class MessageControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var messageSendPort: MessageSendPort

    @MockitoBean
    private lateinit var singleMessageSendUseCasePort: SingleMessageSendUseCasePort

    @MockitoBean
    private lateinit var multipleMessageSendUseCasePort: MultipleMessageSendUseCasePort

    @Test
    @DisplayName("단일 메시지 요청이 들어오면 메시지를 처리하고 상태코드만 반환한다")
    @WithMockUser(username = "dongsu@test.com", roles = ["USER"])
    fun send_single() {
        val sendSingleApi = "/api/v1/message/send"

        //when, then
        sendMockMvc(sendSingleApi, createSingleMsgReq())
            .andExpect(status().isOk())
            .andExpect(content().string(""))
    }

    @Test
    @DisplayName("다중 수신자 문자 요청이 들어와도 상태코드만 반환하는데 이것은 내부적으로 단일 메시지 요청을 사용한다")
    @WithMockUser(username = "dongsu@handsome.com", roles = ["USER"])
    fun send_multi() {
        val sendMultiSendApi = "/api/v1/message/multipleSend"

        //when, then
        sendMockMvc(sendMultiSendApi, createMultiMsgReq())
            .andExpect(status().isOk)
            .andExpect(content().string(""))
    }

    private fun createSingleMsgReq(): String {
        return objectMapper.writeValueAsString(
            mapOf(
                "message" to "simple message",
                "receiverNumber" to "01011112222"
            )
        )
    }

    private fun createMultiMsgReq(): String {
        val list = listOf(
            MessageSendRequest("testMsg", "01011112222"),
            MessageSendRequest("testMsg", "01011113333"),
            MessageSendRequest("testMsg", "01011114444"),
        )
        return objectMapper.writeValueAsString(
            mapOf(
                "requests" to list
            )
        )
    }

    private fun sendMockMvc(api: String, content: String): ResultActions {
        return mockMvc.perform(
            post(api)
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
                .with(csrf())
        )
    }
}