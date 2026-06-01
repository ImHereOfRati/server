package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.application.port.out.FcmTokenPersistencePort
import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.FcmToken
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class FcmTokenEnrollServiceTest {

    @Mock
    private lateinit var fcmTokenPersistencePort: FcmTokenPersistencePort

    private lateinit var fcmTokenEnrollService: FcmTokenEnrollService

    @BeforeEach
    fun setUp() {
        fcmTokenEnrollService = FcmTokenEnrollService(fcmTokenPersistencePort)
    }

    @Test
    @DisplayName("기존에 등록된 토큰이 없으면 새로운 토큰 객체를 생성하여 저장한다")
    fun save_whenNoExistingToken_savesNewToken() {
        // given
        val email = "test@example.com"
        val newFcmToken = "new-fcm-token"
        val deviceType = DeviceType.IOS

        `when`(fcmTokenPersistencePort.findByUserEmail(email)).thenReturn(null)

        // when
        fcmTokenEnrollService.save(email, newFcmToken, deviceType)

        // then
        verify(fcmTokenPersistencePort).findByUserEmail(email)
        verify(fcmTokenPersistencePort).save(
            org.mockito.kotlin.check { token ->
                org.assertj.core.api.Assertions.assertThat(token.email).isEqualTo(email)
                org.assertj.core.api.Assertions.assertThat(token.fcmToken).isEqualTo(newFcmToken)
                org.assertj.core.api.Assertions.assertThat(token.deviceType).isEqualTo(deviceType)
                org.assertj.core.api.Assertions.assertThat(token.id).isNull()
            }
        )
    }

    @Test
    @DisplayName("기존에 등록된 토큰이 있으면 기존 객체의 토큰값을 갱신하여 저장한다")
    fun save_whenExistingToken_updatesAndSavesToken() {
        // given
        val email = "test@example.com"
        val newFcmToken = "updated-fcm-token"
        val deviceType = DeviceType.AOS

        val existingToken = FcmToken(
            id = 1L,
            email = email,
            fcmToken = "old-fcm-token",
            deviceType = deviceType
        )

        `when`(fcmTokenPersistencePort.findByUserEmail(email)).thenReturn(existingToken)

        // when
        fcmTokenEnrollService.save(email, newFcmToken, deviceType)

        // then
        verify(fcmTokenPersistencePort).findByUserEmail(email)
        verify(fcmTokenPersistencePort).save(
            org.mockito.kotlin.check { token ->
                org.assertj.core.api.Assertions.assertThat(token.id).isEqualTo(1L)
                org.assertj.core.api.Assertions.assertThat(token.email).isEqualTo(email)
                org.assertj.core.api.Assertions.assertThat(token.fcmToken).isEqualTo(newFcmToken)
                org.assertj.core.api.Assertions.assertThat(token.deviceType).isEqualTo(deviceType)
            }
        )
    }
}
