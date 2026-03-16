package com.kdongsu5509.imhere.notification.application.port.out

interface SaveTokenPersistencePort {
    fun save(fcmToken: String, userEmail: String)
}