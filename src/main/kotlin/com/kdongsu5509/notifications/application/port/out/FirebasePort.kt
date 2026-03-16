package com.kdongsu5509.imhere.notification.application.port.out

interface FirebasePort {
    fun send(fcmToken: String)
}