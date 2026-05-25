package com.kdongsu5509.notifications.adapter.`in`.web.dto.validation

import com.kdongsu5509.notifications.adapter.`in`.web.dto.MultiNotificationRequest
import com.kdongsu5509.notifications.adapter.`in`.web.dto.NotificationRequest
import com.kdongsu5509.notifications.domain.NotificationMethod
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext


class TargetIdValidator : ConstraintValidator<ValidTargetId, Any> {

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$")
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
            is NotificationRequest -> Pair(value.notificationMethod, listOf(value.targetId))
            is MultiNotificationRequest -> Pair(value.notificationMethod, value.targetIds)
            else -> null
        }
    }

    private fun isValidFormat(type: NotificationMethod, targetId: String): Boolean {
        return when (type) {
            NotificationMethod.USER_EMAIL -> emailRegex.matches(targetId)
            NotificationMethod.PHONE_NUMBER -> phoneRegex.matches(targetId)
        }
    }

    private fun buildErrorMessage(context: ConstraintValidatorContext, type: NotificationMethod) {
        context.disableDefaultConstraintViolation()

        val message = when (type) {
            NotificationMethod.USER_EMAIL -> "올바른 이메일 형식이 아닙니다."
            NotificationMethod.PHONE_NUMBER -> "올바른 휴대전화 번호 형식이 아닙니다."
        }

        context.buildConstraintViolationWithTemplate(message)
            .addConstraintViolation()
    }
}

