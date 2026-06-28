package com.kdongsu5509.notifications.domain

import com.kdongsu5509.notifications.domain.FcmToken.Companion.create
import com.kdongsu5509.notifications.domain.FcmToken.Companion.reconstruct
import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt
import java.time.LocalDateTime

/**
 * FCM 토큰 도메인 객체.
 *
 * - [create]: 신규 등록 시 사용. 이메일·토큰 공백 검증을 수행합니다.
 * - [reconstruct]: 영속성 레이어에서 복원 시 사용. 검증 없이 그대로 복원합니다.
 * - [update]: 새 토큰으로 갱신합니다. 빈 토큰은 허용하지 않습니다.
 */
data class FcmToken internal constructor(
    val id: Long? = null,
    val email: String,
    val fcmToken: String,
    val deviceType: DeviceType,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
) {
    companion object {
        /** 신규 FCM 토큰 생성. 이메일·토큰 공백을 허용하지 않는다. */
        fun create(email: String, fcmToken: String, deviceType: DeviceType): FcmToken {
            requireNotBlank(email, fcmToken)
            return FcmToken(
                id = null,
                email = email,
                fcmToken = fcmToken,
                deviceType = deviceType,
                createdAt = null,
                updatedAt = null
            )
        }

        private fun requireNotBlank(email: String, fcmToken: String) {
            if (email.isBlank() || fcmToken.isBlank()) {
                NotificationException.FCM_TOKEN_EMPTY.throwIt(
                    contextData = mapOf("emailBlank" to email.isBlank(), "tokenBlank" to fcmToken.isBlank())
                )
            }
        }

        /** 영속성 레이어에서 복원 시 사용 */
        fun reconstruct(
            id: Long?,
            email: String,
            fcmToken: String,
            deviceType: DeviceType,
            createdAt: LocalDateTime?,
            updatedAt: LocalDateTime?
        ): FcmToken = FcmToken(id, email, fcmToken, deviceType, createdAt, updatedAt)
    }

    /** 새 토큰 값으로 갱신된 FcmToken을 반환합니다. 빈 토큰은 허용하지 않습니다. */
    fun update(newToken: String): FcmToken {
        if (newToken.isBlank()) {
            NotificationException.FCM_TOKEN_EMPTY.throwIt(contextData = mapOf("tokenBlank" to true))
        }
        return FcmToken(id, email, newToken, deviceType, createdAt, updatedAt)
    }
}
