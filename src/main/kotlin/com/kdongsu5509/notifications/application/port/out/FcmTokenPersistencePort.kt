package com.kdongsu5509.notifications.application.port.out

import com.kdongsu5509.notifications.domain.FcmToken
import java.util.UUID

interface FcmTokenPersistencePort {
    fun save(fcmToken: FcmToken)
    fun findByOwnerId(ownerId: UUID): FcmToken?
    fun deleteById(fcmTokenId: Long)
}
