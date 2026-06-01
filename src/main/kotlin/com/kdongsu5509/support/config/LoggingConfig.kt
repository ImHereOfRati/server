package com.kdongsu5509.support.config

import com.kdongsu5509.support.logger.AccessLogPrinter
import com.kdongsu5509.support.logger.LoggingFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LoggingConfig {

    @Bean
    fun loggingFilter(accessLogPrinter: AccessLogPrinter): LoggingFilter {
        return LoggingFilter(accessLogPrinter)
    }

    @Bean
    fun filterRegistrationBean(loggingFilter: LoggingFilter): FilterRegistrationBean<LoggingFilter> {
        return FilterRegistrationBean(loggingFilter)
    }
}
