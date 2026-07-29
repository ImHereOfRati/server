package com.kdongsu5509.notifications.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class NotificationTypeRenderTest {

    @Test
    @DisplayName("장소 기반 알림은 제목/본문/경로와 FCM data를 조립한다")
    fun render_placeBased() {
        val rendered = NotificationType.LOCATION_TARGET.render(
            senderNickname = "홍길동",
            senderEmail = "gildong@imhere.com",
            extraData = mapOf(NotificationType.PLACE_NAME_KEY to "우리집"),
        )

        assertThat(rendered.type).isEqualTo(NotificationType.LOCATION_TARGET)
        assertThat(rendered.title).isEqualTo("위치 공유 대상자 알림")
        assertThat(rendered.body).contains("홍길동").contains("우리집")
        assertThat(rendered.path).isEqualTo("/record/notifications")
        assertThat(rendered.data)
            .containsEntry("type", "LOCATION_TARGET")
            .containsEntry("path", "/record/notifications")
            .containsEntry("senderNickname", "홍길동")
            .containsEntry("senderEmail", "gildong@imhere.com")
            .containsEntry(NotificationType.PLACE_NAME_KEY, "우리집")
    }

    @Test
    @DisplayName("경로 자리표시자는 extraData 값으로 치환된다")
    fun render_resolvesPathPlaceholder() {
        val rendered = NotificationType.TERMS_UPDATE_NOTICE.render(
            senderNickname = "ImHere",
            senderEmail = "system@imhere.com",
            extraData = mapOf("termId" to "42"),
        )

        assertThat(rendered.path).isEqualTo("/terms-detail/42")
        assertThat(rendered.data).containsEntry("path", "/terms-detail/42")
    }

}
