package com.kdongsu5509.support.config

import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
class TimeZoneConfig {
    init {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    }
}
