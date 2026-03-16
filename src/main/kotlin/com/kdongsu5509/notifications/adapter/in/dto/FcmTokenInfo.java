package com.kdongsu5509.notifications.adapter.in.dto;

import jakarta.validation.constraints.NotBlank;

public record FcmTokenInfo(
        @NotBlank(message = "FCM 토큰은 필수 항목입니다.")
        String fcmToken
) {
}
