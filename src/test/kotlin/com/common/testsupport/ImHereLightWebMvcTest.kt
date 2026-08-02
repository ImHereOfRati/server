package com.common.testsupport

import com.kdongsu5509.auth.security.config.SecurityConfig
import com.kdongsu5509.support.config.LoggingConfig
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.core.annotation.AliasFor
import kotlin.reflect.KClass


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@WebMvcTest(
    excludeFilters = [
        ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [
                SecurityConfig::class,
                LoggingConfig::class
            ]
        )
    ]
)
annotation class ImHereLightWebMvcTest(
    @get:AliasFor(annotation = WebMvcTest::class, attribute = "controllers")
    val controllers: Array<KClass<*>> = []
)
