package com.kdongsu5509.auth.security

import org.springframework.aop.support.StaticMethodMatcherPointcut
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.reflect.Method

class RestControllerMethodPointcut : StaticMethodMatcherPointcut() {

    override fun matches(method: Method, targetClass: Class<*>): Boolean =
        AnnotatedElementUtils.hasAnnotation(targetClass, RestController::class.java) &&
            AnnotatedElementUtils.hasAnnotation(method, RequestMapping::class.java)
}
