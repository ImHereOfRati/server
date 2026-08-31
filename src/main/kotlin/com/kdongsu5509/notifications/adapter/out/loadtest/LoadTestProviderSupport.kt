package com.kdongsu5509.notifications.adapter.out.loadtest

import com.kdongsu5509.support.config.LoadTestProviderProperties
import java.util.concurrent.ThreadLocalRandom

internal class LoadTestProviderSupport(
    private val properties: LoadTestProviderProperties,
) {
    fun delay() {
        if (properties.delayMs > 0) Thread.sleep(properties.delayMs)
    }

    fun shouldFail(): Boolean = properties.failureRate > 0.0 &&
        ThreadLocalRandom.current().nextDouble() < properties.failureRate.coerceIn(0.0, 1.0)

    fun failureMode(): LoadTestProviderProperties.FailureMode = properties.failureMode
}
