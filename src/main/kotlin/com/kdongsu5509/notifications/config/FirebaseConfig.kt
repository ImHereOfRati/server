package com.kdongsu5509.notifications.config

import com.kdongsu5509.imhere.common.config.FcmProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.ClassPathResource
import java.io.InputStream

@Profile("!test")
@Configuration
@EnableConfigurationProperties(FcmProperties::class)
class FirebaseConfig(
    private val fcmProperties: FcmProperties
) {
    @Bean
    fun firebaseApp(): FirebaseApp {
        val resource = ClassPathResource(fcmProperties.path)
        val serviceAccount: InputStream = resource.inputStream

        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .build()

        return FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging {
        return FirebaseMessaging.getInstance(firebaseApp)
    }
}