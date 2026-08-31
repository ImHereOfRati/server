package com.kdongsu5509.notifications.adapter.out.loadtest

import com.kdongsu5509.notifications.application.port.out.FirebasePort
import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.RenderedNotification
import com.kdongsu5509.notifications.exception.RetryableFcmException
import com.kdongsu5509.support.config.LoadTestProviderProperties
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("loadtest")
class LoadTestFirebaseAdapter(
    properties: LoadTestProviderProperties,
) : FirebasePort {
    private val support = LoadTestProviderSupport(properties)

    override fun send(fcmToken: String, deviceType: DeviceType, rendered: RenderedNotification) {
        support.delay()
        if (support.shouldFail()) {
            throw RetryableFcmException("load-test FCM provider failure (${support.failureMode()})")
        }
    }
}
