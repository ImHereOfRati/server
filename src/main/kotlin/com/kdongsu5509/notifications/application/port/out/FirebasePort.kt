package com.kdongsu5509.notifications.application.port.out

import com.kdongsu5509.notifications.domain.DeviceType
import com.kdongsu5509.notifications.domain.RenderedNotification

interface FirebasePort {
    /**
     * 렌더링된 알림을 한 기기로 보낸다.
     *
     * 제목·본문·data 맵을 따로 받던 예전 시그니처는 어댑터가 `data["type"]` 문자열에서 알림 종류를 되짚어야 했다.
     * [RenderedNotification]을 통째로 받으면 종류와 전달 정책이 그대로 따라오므로 그 역파싱이 필요 없다.
     * [deviceType]은 Android/iOS 중 어느 쪽 전달 정책을 적용할지 고르는 데 쓴다.
     */
    fun send(fcmToken: String, deviceType: DeviceType, rendered: RenderedNotification)
}
