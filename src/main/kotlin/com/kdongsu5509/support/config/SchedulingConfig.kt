package com.kdongsu5509.support.config

import org.springframework.context.annotation.Configuration
import org.springframework.resilience.annotation.EnableResilientMethods
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableResilientMethods
@EnableScheduling
class SchedulingConfig
