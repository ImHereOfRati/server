package com.kdongsu5509.notifications.adapter.`in`.web.dto.validation

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [TargetIdValidator::class])
annotation class ValidTargetId(
    val message: String = "발송 대상 타입에 맞지 않는 식별자 형식입니다.",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
