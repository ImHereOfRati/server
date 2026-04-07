package com.kdongsu5509.notifications.application.serivce

import com.kdongsu5509.notifications.application.port.out.SaveOrUpdateTokenPersistencePort
import com.kdongsu5509.notifications.domain.DeviceType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class ManageFcmTokenServiceTest {

    @Mock
    private lateinit var saveOrUpdateTokenPersistencePort: SaveOrUpdateTokenPersistencePort

    @InjectMocks
    private lateinit var manageFcmTokenService: ManageFcmTokenService

    companion object {
        const val USER_EMAIL = "user@example.com"
        const val FCM_TOKEN = "sample-fcm-token"
    }

    @Test
    @DisplayName("FCM 토큰 저장 또는 업데이트 성공")
    fun save_success() {
        // when
        manageFcmTokenService.save(USER_EMAIL, FCM_TOKEN, DeviceType.AOS)

        // then
        verify(saveOrUpdateTokenPersistencePort).saveOrUpdate(FCM_TOKEN, USER_EMAIL, DeviceType.AOS)
    }
}
