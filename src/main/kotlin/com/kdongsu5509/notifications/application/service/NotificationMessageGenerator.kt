package com.kdongsu5509.notifications.application.service

import com.kdongsu5509.notifications.adapter.`in`.messageQueue.dto.NotificationType


object NotificationMessageGenerator {

    fun getMessageTitle(type: NotificationType): String {
        return when (type) {
            NotificationType.FRIEND_REQUEST_RECEIVED -> "새로운 친구 요청"
            NotificationType.FRIEND_REQUEST_ACCEPTED -> "친구 요청 수락"
            NotificationType.LOCATION_SHARE_RECEIVED -> "위치 공유 알림"
            NotificationType.ARRIVAL, NotificationType.ARRIVAL_CONFIRMATION -> "도착 안내"
            NotificationType.DEPARTURE -> "출발 안내"
            NotificationType.TERMS_UPDATE_NOTICE -> "서비스 공지사항"
            NotificationType.DELIVERY_RESULT_NOTICE -> "발송 결과 알림"
            NotificationType.DELIVERY_FAILED_NOTICE -> "발송 실패 알림"
        }
    }

    fun getMessageBody(type: NotificationType, senderNickname: String): String {
        return when (type) {
            NotificationType.FRIEND_REQUEST_RECEIVED -> "${senderNickname}님이 친구 요청을 보냈습니다."
            NotificationType.FRIEND_REQUEST_ACCEPTED -> "${senderNickname}님이 친구 요청을 수락했습니다."
            NotificationType.LOCATION_SHARE_RECEIVED -> "${senderNickname}님이 위치를 공유했습니다."
            NotificationType.ARRIVAL, NotificationType.ARRIVAL_CONFIRMATION -> "${senderNickname}님이 도착했습니다."
            NotificationType.DEPARTURE -> "${senderNickname}님이 출발했습니다."
            NotificationType.TERMS_UPDATE_NOTICE -> "이용약관이 업데이트되었습니다. 내용을 확인해 주세요."
            NotificationType.DELIVERY_RESULT_NOTICE -> "요청하신 발송 작업이 완료되었습니다."
            NotificationType.DELIVERY_FAILED_NOTICE -> "요청하신 발송 작업이 실패했습니다. 잠시 후 다시 시도해 주세요."
        }
    }
}
