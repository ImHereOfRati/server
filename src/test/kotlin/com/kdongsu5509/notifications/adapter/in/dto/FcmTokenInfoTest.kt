package com.kdongsu5509.notifications.adapter.`in`.dto

import com.kdongsu5509.notifications.adapter.`in`.web.dto.FcmTokenInfo
import com.kdongsu5509.notifications.domain.DeviceType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FcmTokenInfoTest {

    @Test
    @DisplayName("fcm 토큰이 없으면 오류 발생")
    fun blank_token() {
        //given
        val emptyToken = ""
        val testDeviceType = DeviceType.AOS
        //when, then
        Assertions.assertThatThrownBy {
            FcmTokenInfo(emptyToken, testDeviceType)
        }
    }

    @Test
    @DisplayName("잘 생성함")
    fun construct_good() {
        val token = "valid-token"
        val testDeviceType = DeviceType.AOS
        assertDoesNotThrow {
            FcmTokenInfo(token, testDeviceType)
        }
    }
}
