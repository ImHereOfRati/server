package com.kdongsu5509.admin

import org.springframework.boot.health.actuate.endpoint.HealthDescriptor
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint
import org.springframework.boot.actuate.info.InfoEndpoint
import org.springframework.stereotype.Service

@Service
class AdminOperationalStatus(
    private val healthEndpoint: HealthEndpoint,
    private val infoEndpoint: InfoEndpoint,
) {
    fun health(): HealthDescriptor = healthEndpoint.health()

    fun info(): Map<String, Any> = infoEndpoint.info()
}
