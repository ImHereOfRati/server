package com.kdongsu5509.notifications.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public record FcmToken(
        Long id,
        UUID userId,
        String fcmToken,
        DeviceType deviceType,
        LocalDateTime updatedAt
) {
}
