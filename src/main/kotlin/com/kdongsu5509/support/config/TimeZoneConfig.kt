package com.kdongsu5509.support.config

import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class TimeZoneConfig {
    companion object {
        val SEOUL_TIMEZONE_ID: String = "Asia/Seoul"
    }

    init {
        val koreaTimeZone = TimeZone.getTimeZone(SEOUL_TIMEZONE_ID)
        TimeZone.setDefault(koreaTimeZone)
    }
}
