package com.kdongsu5509.notifications.adapter.`in`.web.dto.validation

import com.kdongsu5509.notifications.adapter.`in`.web.dto.NotificationRequest
import com.kdongsu5509.notifications.domain.NotificationMethod
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import java.util.*

class TargetIdValidator : ConstraintValidator<ValidTargetId, Any> {

    private val phoneRegex = Regex("^01[0-9]-?[0-9]{3,4}-?[0-9]{4}\$")

    override fun isValid(value: Any?, context: ConstraintValidatorContext): Boolean {
        if (value == null) return true

        // 1. 검증할 데이터 추출
        val (type, ids) = extractTargetInfo(value) ?: return true

        // 2. 모든 ID가 지정된 타입의 형식에 맞는지 검사
        val isValid = ids.all { isValidFormat(type, it) }

        // 3. 검증 실패 시 타입에 맞는 에러 메시지 생성
        if (!isValid) {
            buildErrorMessage(context, type)
        }

        return isValid
    }

    private fun extractTargetInfo(value: Any): Pair<NotificationMethod, List<String>>? {
        return when (value) {
            is NotificationRequest -> Pair(value.notificationMethod, value.targetIds)
            else -> null
        }
    }

    private fun isValidFormat(type: NotificationMethod, targetId: String): Boolean {
        return when (type) {
            NotificationMethod.FCM -> isUuid(targetId)
            NotificationMethod.SMS -> phoneRegex.matches(targetId)
        }
    }

    private fun isUuid(value: String): Boolean =
        try {
            UUID.fromString(value)
            true
        } catch (_: IllegalArgumentException) {
            false
        }

    private fun buildErrorMessage(context: ConstraintValidatorContext, type: NotificationMethod) {
        context.disableDefaultConstraintViolation()

        val message = when (type) {
            NotificationMethod.FCM -> "올바른 사용자 ID 형식이 아닙니다."
            NotificationMethod.SMS -> "올바른 휴대전화 번호 형식이 아닙니다."
        }

        context.buildConstraintViolationWithTemplate(message)
            .addConstraintViolation()
    }
}

