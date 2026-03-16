package com.kdongsu5509.imhere

import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile

@Profile("test")
@TestConfiguration
class TestFirebaseConfig {
    @Bean
    @Primary
    fun firebaseApp(): FirebaseApp {
        return Mockito.mock(FirebaseApp::class.java)
    }

    @Bean
    @Primary
    fun firebaseMessaging(): FirebaseMessaging {
        return Mockito.mock(FirebaseMessaging::class.java)
    }
}