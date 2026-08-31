package com.kdongsu5509.notifications.adapter.out.loadtest

import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.NotificationTemplate
import com.kdongsu5509.notifications.domain.NotificationType
import com.kdongsu5509.notifications.domain.SMS
import com.kdongsu5509.notifications.exception.RetryableFcmException
import com.kdongsu5509.support.config.LoadTestProviderProperties
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class LoadTestProviderAdapterTest {
    @Test
    fun `SMS test provider does not call an external service and returns success`() {
        val adapter = LoadTestExternalMessageAdapter(LoadTestProviderProperties())

        val result = adapter.send(SMS("sender", "01000000000", "load test"))

        assertThat(result.isSuccess).isTrue()
        assertThat(result.providerMessageId).startsWith("loadtest-sms-")
    }

    @Test
    fun `configured FCM failure follows retryable application path`() {
        val adapter = LoadTestFirebaseAdapter(
            LoadTestProviderProperties(failureRate = 1.0),
        )

        assertThatThrownBy {
            adapter.send(
                "token",
                DeviceType.AOS,
                NotificationTemplate.render(NotificationType.ARRIVAL, "sender", mapOf("placeName" to "test")),
            )
        }.isInstanceOf(RetryableFcmException::class.java)
    }
}
