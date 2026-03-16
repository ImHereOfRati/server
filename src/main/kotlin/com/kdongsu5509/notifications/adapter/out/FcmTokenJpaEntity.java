package com.kdongsu5509.notifications.adapter.out;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;

import com.kdongsu5509.notifications.domain.DeviceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fcm_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmTokenJpaEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(columnDefinition = "BINARY(16)", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String token;

    @Enumerated(STRING)
    @Column(nullable = false)
    private DeviceType deviceType;

    @Builder
    public FcmTokenJpaEntity(UUID userId, String token, DeviceType deviceType) {
        this.userId = userId;
        this.token = token;
        this.deviceType = deviceType;
    }
}
