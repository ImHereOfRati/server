package com.kdongsu5509.notifications.adapter.`in`.web.dto.validation

import com.kdongsu5509.notifications.adapter.`in`.web.dto.NotificationRequest
import com.kdongsu5509.notifications.domain.NotificationMethod
import com.kdongsu5509.notifications.domain.NotificationType
import jakarta.validation.ConstraintValidatorContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import java.util.*

class TargetIdValidatorTest {

    private lateinit var validator: TargetIdValidator

    @Mock
    private lateinit var context: ConstraintValidatorContext

    @Mock
    private lateinit var builder: ConstraintValidatorContext.ConstraintViolationBuilder

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        validator = TargetIdValidator()
    }

    private fun setupContextMock(message: String) {
        `when`(context.buildConstraintViolationWithTemplate(message)).thenReturn(builder)
        `when`(builder.addConstraintViolation()).thenReturn(context)
    }

    @Test
    @DisplayName("null이 들어오면 true를 반환한다 (NotNull 어노테이션이 검증할 부분)")
    fun isValid_null() {
        assertThat(validator.isValid(null, context)).isTrue()
    }

    @Test
    @DisplayName("알 수 없는 타입의 객체가 들어오면 true를 반환한다")
    fun isValid_unknownType() {
        assertThat(validator.isValid("Just String", context)).isTrue()
    }

    @Test
    @DisplayName("NotificationMethod가 FCM일 때 올바른 사용자 ID 형식이면 true를 반환한다")
    fun isValid_userId_success() {
        val request = NotificationRequest(
            targetIds = listOf(UUID.randomUUID().toString()),
            notificationMethod = NotificationMethod.FCM,
            type = NotificationType.FRIEND_REQUEST_RECEIVED
        )
        assertThat(validator.isValid(request, context)).isTrue()
    }

    @Test
    @DisplayName("NotificationMethod가 FCM일 때 사용자 ID가 아니면 false를 반환한다")
    fun isValid_userId_fail() {
        val request = NotificationRequest(
            targetIds = listOf("test@example.com"), // 더 이상 이메일로 지목하지 않는다
            notificationMethod = NotificationMethod.FCM,
            type = NotificationType.FRIEND_REQUEST_RECEIVED
        )
        setupContextMock("올바른 사용자 ID 형식이 아닙니다.")

        assertThat(validator.isValid(request, context)).isFalse()
        verify(context).disableDefaultConstraintViolation()
        verify(context).buildConstraintViolationWithTemplate("올바른 사용자 ID 형식이 아닙니다.")
    }

    @Test
    @DisplayName("NotificationMethod가 휴대전화일 때 올바른 형식이면 true를 반환한다")
    fun isValid_phone_success() {
        val request1 = NotificationRequest(
            targetIds = listOf("010-1234-5678"),
            notificationMethod = NotificationMethod.SMS,
            type = NotificationType.FRIEND_REQUEST_RECEIVED
        )
        val request2 = NotificationRequest(
            targetIds = listOf("01012345678"),
            notificationMethod = NotificationMethod.SMS,
            type = NotificationType.FRIEND_REQUEST_RECEIVED
        )
        assertThat(validator.isValid(request1, context)).isTrue()
        assertThat(validator.isValid(request2, context)).isTrue()
    }

    @Test
    @DisplayName("NotificationMethod가 휴대전화일 때 잘못된 형식이면 false를 반환한다")
    fun isValid_phone_fail() {
        val request = NotificationRequest(
            targetIds = listOf("02-123-4567"), // Not 01X
            notificationMethod = NotificationMethod.SMS,
            type = NotificationType.FRIEND_REQUEST_RECEIVED
        )
        setupContextMock("올바른 휴대전화 번호 형식이 아닙니다.")

        assertThat(validator.isValid(request, context)).isFalse()
        verify(context).disableDefaultConstraintViolation()
        verify(context).buildConstraintViolationWithTemplate("올바른 휴대전화 번호 형식이 아닙니다.")
    }

    @Test
    @DisplayName("NotificationRequest의 타겟 중 하나라도 잘못되면 false를 반환한다")
    fun isValid_multi_fail() {
        val request = NotificationRequest(
            targetIds = listOf(UUID.randomUUID().toString(), "not-a-user-id"),
            notificationMethod = NotificationMethod.FCM,
            type = NotificationType.FRIEND_REQUEST_RECEIVED
        )
        setupContextMock("올바른 사용자 ID 형식이 아닙니다.")

        assertThat(validator.isValid(request, context)).isFalse()
    }

    @Test
    @DisplayName("NotificationRequest의 타겟이 모두 맞으면 true를 반환한다")
    fun isValid_multi_success() {
        val request = NotificationRequest(
            targetIds = listOf(UUID.randomUUID().toString(), UUID.randomUUID().toString()),
            notificationMethod = NotificationMethod.FCM,
            type = NotificationType.FRIEND_REQUEST_RECEIVED
        )
        assertThat(validator.isValid(request, context)).isTrue()
    }
}
