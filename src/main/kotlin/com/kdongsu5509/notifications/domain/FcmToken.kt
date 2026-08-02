package com.kdongsu5509.notifications.domain

import com.kdongsu5509.notifications.exception.NotificationException
import com.kdongsu5509.support.exception.throwIt
import java.time.LocalDateTime
import java.util.UUID

data class FcmToken(
    val id: Long? = null,
    val ownerId: UUID,
    val fcmToken: String,
    val deviceType: DeviceType,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null
) {
    init {
        validateRequiredFields()
    }

    fun update(newToken: String): FcmToken = copy(fcmToken = newToken)

    private fun validateRequiredFields() {
        if (fcmToken.isBlank()) {
            NotificationException.FCM_TOKEN_EMPTY.throwIt(
                contextData = mapOf(
                    "ownerId" to ownerId,
                    "tokenBlank" to true
                )
            )
        }
    }
}
