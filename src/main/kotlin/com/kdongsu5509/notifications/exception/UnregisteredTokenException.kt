package com.kdongsu5509.notifications.exception

import com.kdongsu5509.support.exception.ImHereBaseException

/**
 * 등록 해제/만료된 FCM 토큰. 발송 실패이자 "이 토큰을 정리하라"는 신호를 명시적 타입으로 표현한다.
 *
 * 과거에는 예외의 `contextData["unregistered"]` 문자열 키로 이 사실을 실어 날랐으나(stringly-typed 프로토콜),
 * 전용 예외 타입으로 대체해 발송 서비스가 타입으로 판별하고 토큰을 삭제할 수 있게 한다.
 */
class UnregisteredTokenException(
    message: String = "등록 해제된 FCM 토큰입니다.",
    cause: Throwable? = null,
) : ImHereBaseException(
    errorCode = NotificationException.FCM_TOKEN_UNREGISTERED,
    overrideMessage = message,
    cause = cause,
)
