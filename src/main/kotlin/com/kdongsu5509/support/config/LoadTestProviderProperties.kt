package com.kdongsu5509.support.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "loadtest.provider")
data class LoadTestProviderProperties(
    val delayMs: Long = 0,
    val failureRate: Double = 0.0,
    val failureMode: FailureMode = FailureMode.RETRYABLE,
) {
    enum class FailureMode {
        RETRYABLE,
        REJECTED,
        UNKNOWN,
    }
}
