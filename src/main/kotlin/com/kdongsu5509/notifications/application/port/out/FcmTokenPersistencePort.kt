package com.kdongsu5509.notifications.application.port.out

import com.kdongsu5509.notifications.domain.FcmToken

interface FcmTokenPersistencePort {
    fun save(fcmToken: FcmToken)
    fun findByUserEmail(userEmail: String): FcmToken?
    fun deleteById(fcmTokenId: Long)
}
