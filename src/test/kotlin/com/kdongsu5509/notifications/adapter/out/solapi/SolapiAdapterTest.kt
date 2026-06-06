package com.kdongsu5509.notifications.adapter.out.solapi

import com.kdongsu5509.notifications.domain.SMS
import com.kdongsu5509.support.config.ExternalSMSProperties
import com.solapi.sdk.message.exception.SolapiBadRequestException
import com.solapi.sdk.message.exception.SolapiInvalidApiKeyException
import com.solapi.sdk.message.exception.SolapiMessageNotReceivedException
import com.solapi.sdk.message.model.Message
import com.solapi.sdk.message.service.DefaultMessageService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class SolapiAdapterTest {

    @Mock
    private lateinit var properties: ExternalSMSProperties

    @Mock
    private lateinit var solapiService: DefaultMessageService

    private lateinit var adapter: SolapiAdapter

    @BeforeEach
    fun setUp() {
        org.mockito.Mockito.lenient().`when`(properties.sender).thenReturn("01012345678")
        adapter = SolapiAdapter(properties, solapiService)
    }

    @Test
    @DisplayName("단일 문자 발송 성공 시 success 응답을 반환한다")
    fun send_success() {
        val sms = SMS("01011112222", "TestLocation", "TestSender")

        val sendMethod = DefaultMessageService::class.java.methods.find {
            it.name == "send" && it.parameterTypes.size == 2 && it.parameterTypes[0] == Message::class.java
        }!!
        val mockResponse = mock(sendMethod.returnType)

        Mockito.doReturn(mockResponse).`when`(solapiService).send(any<Message>(), isNull())

        val result = adapter.send(sms)
        assertThat(result.status).isEqualTo(SolapiResponse.SUCCESS_STATUS)
    }

    @Test
    @DisplayName("단일 문자 발송 실패 시 fail 응답을 반환한다")
    fun send_fail() {
        val sms = SMS("01011112222", "TestLocation", "TestSender")
        Mockito.doThrow(RuntimeException("Test Exception")).`when`(solapiService).send(any<Message>(), isNull())

        val result = adapter.send(sms)
        assertThat(result.status).isEqualTo(SolapiResponse.FAIL_STATUS)
        assertThat(result.message).contains("Test Exception")
    }

    @Test
    @DisplayName("다중 문자 발송 실패 시 예외를 잡아 fail 응답 리스트를 반환한다")
    fun sendMultiple_fail() {
        val smsList = listOf(SMS("01011112222", "L1", "S1"), SMS("01011113333", "L2", "S2"))
        Mockito.doThrow(RuntimeException("Bulk fail")).`when`(solapiService).send(any<List<Message>>(), isNull())

        val result = adapter.sendMultiple(smsList)
        assertThat(result).hasSize(2)
        assertThat(result[0].status).isEqualTo(SolapiResponse.FAIL_STATUS)
        assertThat(result[1].status).isEqualTo(SolapiResponse.FAIL_STATUS)
    }

    @Test
    @DisplayName("다중 문자 발송 시 리스트가 비어있으면 InvalidInputException 예외를 발생시킨다")
    fun sendMultiple_empty() {
        assertThatThrownBy {
            adapter.sendMultiple(emptyList())
        }.isInstanceOf(com.kdongsu5509.support.exception.type.InvalidInputException::class.java)
    }

    @Test
    @DisplayName("다중 문자 발송 시 응답 데이터가 비어있으면 실패 응답을 반환한다")
    fun sendMultiple_emptyResponse() {
        val smsList = listOf(SMS("01011112222", "L1", "S1"))

        doAnswer {
            val sendMethod = DefaultMessageService::class.java.methods.find {
                it.name == "send" && it.parameterTypes.size == 2 && it.parameterTypes[0] == List::class.java
            }!!
            val mockResult = mock(sendMethod.returnType)
            `when`(mockResult.javaClass.getMethod("getMessageList").invoke(mockResult)).thenReturn(emptyList<Any>())
            mockResult
        }.`when`(solapiService).send(any<List<Message>>(), isNull())

        val result = adapter.sendMultiple(smsList)
        assertThat(result).hasSize(1)
        assertThat(result[0].status).isEqualTo(SolapiResponse.FAIL_STATUS)
        assertThat(result[0].message).contains("응답 데이터가 비어있습니다.")
    }

    @Test
    @DisplayName("다중 문자 발송 시 일부는 성공하고 일부는 실패하는 경우를 매핑한다")
    fun sendMultiple_partialSuccess() {
        val smsList = listOf(SMS("01011112222", "L1", "S1"), SMS("01011113333", "L2", "S2"))

        doAnswer {
            val sendMethod = DefaultMessageService::class.java.methods.find {
                it.name == "send" && it.parameterTypes.size == 2 && it.parameterTypes[0] == java.util.List::class.java
            }!!
            val mockResult = mock(sendMethod.returnType)

            val modelClass =
                Class.forName("com.solapi.sdk.message.dto.response.MultipleDetailMessageSentResponse\$MessageList")

            val successDetail = Mockito.mock(modelClass, Mockito.withSettings().defaultAnswer { inv ->
                val name = inv.method.name
                if (name == "getStatusCode" || name == "statusCode" || name == "getStatusCode\$solapi_messaging") "2000"
                else null
            })

            val failDetail = Mockito.mock(modelClass, Mockito.withSettings().defaultAnswer { inv ->
                val name = inv.method.name
                if (name == "getStatusCode" || name == "statusCode" || name == "getStatusCode\$solapi_messaging") "4004"
                else if (name == "getStatusMessage" || name == "statusMessage" || name == "getStatusMessage\$solapi_messaging") "Not Found"
                else null
            })

            val list = listOf(successDetail, failDetail)
            Mockito.`when`(mockResult.javaClass.getMethod("getMessageList").invoke(mockResult)).thenReturn(list)
            mockResult
        }.`when`(solapiService).send(any<List<Message>>(), isNull())

        val result = adapter.sendMultiple(smsList)
        assertThat(result).hasSize(2)
        assertThat(result[0].status).isEqualTo(SolapiResponse.SUCCESS_STATUS)
        assertThat(result[1].status).isEqualTo(SolapiResponse.FAIL_STATUS)
        assertThat(result[1].message).contains("Not Found")
    }

    @Test
    @DisplayName("Solapi API 에러에 따라 적절한 에러 메시지를 반환한다")
    fun handleException_branches() {
        val sms = SMS("01011112222", "L1", "S1")

        val badReq = mock(SolapiBadRequestException::class.java)
        whenever(badReq.message).thenReturn("Bad Req")
        doAnswer { throw badReq }.`when`(solapiService).send(any<Message>(), isNull())
        assertThat(adapter.send(sms).message).contains("잘못된 요청: Bad Req")

        val invKey = mock(SolapiInvalidApiKeyException::class.java)
        whenever(invKey.message).thenReturn("Inv Key")
        doAnswer { throw invKey }.`when`(solapiService).send(any<Message>(), isNull())
        assertThat(adapter.send(sms).message).contains("잘못된 API 키: Inv Key")

        val notRec = mock(SolapiMessageNotReceivedException::class.java)
        whenever(notRec.message).thenReturn("Not Rec")
        doAnswer { throw notRec }.`when`(solapiService).send(any<Message>(), isNull())
        assertThat(adapter.send(sms).message).contains("발송 미접수: Not Rec")
    }
}
